package de.knurl.feature.nutrition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.knurl.core.food.OpenFoodFacts
import de.knurl.data.nutrition.NutritionRepository
import de.knurl.domain.calc.Nutrition
import de.knurl.domain.model.Food
import de.knurl.domain.model.FoodEntry
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Online search: idle (too short), loading, done, or failed (offline). */
sealed interface OnlineSearch {
    data object Idle : OnlineSearch
    data object Loading : OnlineSearch
    data class Done(val foods: List<Food>) : OnlineSearch
    data object Failed : OnlineSearch
}

/** What happened to a scanned barcode. */
sealed interface ScanResult {
    data object LookingUp : ScanResult
    data class Found(val food: Food) : ScanResult
    data class NotFound(val barcode: String) : ScanResult
    data object Failed : ScanResult
}

data class AddFoodState(val query: String, val saved: List<Food>, val online: OnlineSearch)

@OptIn(FlowPreview::class)
class AddFoodViewModel(private val nutrition: NutritionRepository, private val german: Boolean, private val day: String) : ViewModel() {

    private val query = MutableStateFlow("")
    private val online = MutableStateFlow<OnlineSearch>(OnlineSearch.Idle)

    private val _scan = MutableStateFlow<ScanResult?>(null)
    val scan: StateFlow<ScanResult?> = _scan.asStateFlow()

    val state: StateFlow<AddFoodState> = combine(query, nutrition.data, online) { q, data, o ->
        val saved = data?.foods.orEmpty()
        val words = q.trim().lowercase().split(' ').filter { it.isNotBlank() }
        AddFoodState(
            query = q,
            saved = if (words.isEmpty()) saved.take(30) else saved.filter { f -> words.all { w -> "${f.name} ${f.brand}".lowercase().contains(w) } },
            online = o,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AddFoodState("", emptyList(), OnlineSearch.Idle))

    init {
        // Open Food Facts only from three letters on and once typing pauses: one request, not one per key.
        viewModelScope.launch {
            query.map { it.trim() }.distinctUntilChanged().debounce(500).collectLatest { q ->
                if (q.length < 3) { online.value = OnlineSearch.Idle; return@collectLatest }
                online.value = OnlineSearch.Loading
                online.value = runCatching { OpenFoodFacts.search(q, german) }
                    .fold({ OnlineSearch.Done(it) }, { OnlineSearch.Failed })
            }
        }
    }

    fun setQuery(value: String) {
        query.value = value
    }

    /** A barcode from the scanner: the saved food if known, otherwise Open Food Facts. */
    fun lookUp(barcode: String) {
        nutrition.data.value?.foods?.firstOrNull { it.barcode == barcode }?.let { _scan.value = ScanResult.Found(it); return }
        _scan.value = ScanResult.LookingUp
        viewModelScope.launch {
            _scan.value = runCatching { OpenFoodFacts.byBarcode(barcode, german) }
                .fold({ food -> if (food != null) ScanResult.Found(food) else ScanResult.NotFound(barcode) }, { ScanResult.Failed })
        }
    }

    fun clearScan() {
        _scan.value = null
    }

    /** Logs [grams] of [food] on the day and keeps the food for next time. */
    fun log(food: Food, grams: Double) {
        val now = System.currentTimeMillis()
        nutrition.saveFood(food.copy(lastUsedMs = now))
        nutrition.log(Nutrition.portion(food, grams, nutrition.newId(), day, now))
    }

    /** Just calories and protein, without a food behind them. */
    fun quickAdd(name: String, kcal: Int, protein: Double) {
        nutrition.log(FoodEntry(nutrition.newId(), day, System.currentTimeMillis(), name, kcal, protein))
    }

    /** A food typed in by hand (optionally for a scanned barcode), ready to pick an amount. */
    fun ownFood(name: String, kcalPer100: Double, proteinPer100: Double, carbs: Double?, fat: Double?, barcode: String?): Food =
        Food(
            id = barcode?.let { "off_$it" } ?: "own_${nutrition.newId()}",
            name = name.trim().take(120),
            barcode = barcode,
            kcalPer100 = kcalPer100,
            proteinPer100 = proteinPer100,
            carbsPer100 = carbs,
            fatPer100 = fat,
        )
}
