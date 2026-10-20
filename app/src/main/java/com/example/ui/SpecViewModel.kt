package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.SpecBoardApplication
import com.example.data.entity.ClipboardItem
import com.example.data.entity.PhoneField
import com.example.data.entity.PhoneRecord
import com.example.data.repository.DuplicateAction
import com.example.data.repository.SpecRepository
import com.example.model.SmartphoneSpecDef
import com.example.parser.ParsedPhoneData
import com.example.parser.TsvParser
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DuplicateQueueItem(
    val parsedPhone: ParsedPhoneData,
    val existingPhone: PhoneRecord
)

class SpecViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: SpecRepository =
        (application as? SpecBoardApplication)?.repository
            ?: SpecBoardApplication.instance.repository

    // State
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedBrand = MutableStateFlow<String?>(null)
    val selectedBrand: StateFlow<String?> = _selectedBrand.asStateFlow()

    private val _activePhoneId = MutableStateFlow<Long?>(null)
    val activePhoneId: StateFlow<Long?> = _activePhoneId.asStateFlow()

    private val _pendingDuplicates = MutableStateFlow<List<DuplicateQueueItem>>(emptyList())
    val pendingDuplicates: StateFlow<List<DuplicateQueueItem>> = _pendingDuplicates.asStateFlow()

    private val _userMessage = MutableStateFlow<String?>(null)
    val userMessage: StateFlow<String?> = _userMessage.asStateFlow()

    // Config
    private val _columnConfig = MutableStateFlow(SmartphoneSpecDef.ColumnMappingConfig())
    val columnConfig: StateFlow<SmartphoneSpecDef.ColumnMappingConfig> = _columnConfig.asStateFlow()

    val allPhones: StateFlow<List<PhoneRecord>> = repository.allPhones.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allBrands: StateFlow<List<String>> = repository.allBrands.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val favoritePhones: StateFlow<List<PhoneRecord>> = repository.favoritePhones.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val recentPhones: StateFlow<List<PhoneRecord>> = repository.recentPhones.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allClipboard: StateFlow<List<ClipboardItem>> = repository.allClipboard.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )



    // Filtered Phones based on search query & brand, searching both phone metadata and specifications
    @OptIn(ExperimentalCoroutinesApi::class)
    val filteredPhones: StateFlow<List<PhoneRecord>> = _searchQuery
        .flatMapLatest { query ->
            if (query.isBlank()) {
                repository.allPhones
            } else {
                repository.searchPhonesWithSpecs(query.trim())
            }
        }
        .combine(_selectedBrand) { phones, brand ->
            if (brand != null) {
                phones.filter { it.brand.equals(brand, ignoreCase = true) }
            } else {
                phones
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedBrand(brand: String?) {
        _selectedBrand.value = brand
    }

    fun setActivePhoneId(id: Long?) {
        _activePhoneId.value = id
    }

    fun clearMessage() {
        _userMessage.value = null
    }

    fun getPhoneDetails(phoneId: Long) = repository.getPhoneById(phoneId)

    fun getFieldsForPhone(phoneId: Long) = repository.getFieldsForPhone(phoneId)

    fun getFieldsByCategory(phoneId: Long, category: String) =
        repository.getFieldsByCategory(phoneId, category)

    fun togglePhoneFavorite(phoneId: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.togglePhoneFavorite(phoneId, isFav)
        }
    }

    fun togglePhonePinned(phoneId: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.togglePhonePinned(phoneId, isPinned)
        }
    }

    fun deletePhone(phoneId: Long) {
        viewModelScope.launch {
            repository.deletePhone(phoneId)
            _userMessage.value = "Phone deleted"
        }
    }

    fun updateField(field: PhoneField) {
        viewModelScope.launch {
            repository.updateField(field)
        }
    }

    fun toggleFieldFavorite(fieldId: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleFieldFavorite(fieldId, isFav)
        }
    }

    // Process TSV raw text
    fun processTsvImport(tsvText: String) {
        viewModelScope.launch {
            val detection = TsvParser.parseTsv(tsvText, _columnConfig.value)
            if (!detection.isTsv || detection.parsedPhones.isEmpty()) {
                _userMessage.value = "No valid smartphone specification rows detected."
                return@launch
            }

            // Also record to clipboard history
            repository.addClipboardItem(
                text = tsvText,
                isTsv = true,
                detectedCount = detection.parsedPhones.size
            )

            val duplicates = mutableListOf<DuplicateQueueItem>()
            var autoImportedCount = 0

            for (phone in detection.parsedPhones) {
                val existing = repository.checkDuplicate(phone.brand, phone.model, phone.fullName)
                if (existing != null) {
                    duplicates.add(DuplicateQueueItem(phone, existing))
                } else {
                    repository.insertPhoneWithFields(phone)
                    autoImportedCount++
                }
            }

            if (duplicates.isNotEmpty()) {
                _pendingDuplicates.value = duplicates
            } else {
                _userMessage.value = "Successfully imported $autoImportedCount smartphone(s)!"
            }
        }
    }

    fun resolveDuplicate(action: DuplicateAction) {
        val currentQueue = _pendingDuplicates.value
        if (currentQueue.isEmpty()) return

        val currentItem = currentQueue.first()
        viewModelScope.launch {
            repository.insertPhoneWithFields(
                parsed = currentItem.parsedPhone,
                duplicateAction = action,
                targetPhoneId = currentItem.existingPhone.id
            )
            _pendingDuplicates.value = currentQueue.drop(1)
            if (_pendingDuplicates.value.isEmpty()) {
                _userMessage.value = "All smartphone specifications processed!"
            }
        }
    }

    // Clipboard actions
    fun addClipboardText(text: String) {
        viewModelScope.launch {
            val isTsv = TsvParser.isLikelyTsv(text)
            val detection = if (isTsv) TsvParser.parseTsv(text, _columnConfig.value) else null
            val count = detection?.parsedPhones?.size ?: 0
            repository.addClipboardItem(text, isTsv, count)
        }
    }

    fun deleteClipboardItem(id: Long) {
        viewModelScope.launch {
            repository.deleteClipboardItem(id)
        }
    }

    fun toggleClipboardFavorite(id: Long, isFav: Boolean) {
        viewModelScope.launch {
            repository.toggleClipboardFavorite(id, isFav)
        }
    }

    fun toggleClipboardPinned(id: Long, isPinned: Boolean) {
        viewModelScope.launch {
            repository.toggleClipboardPinned(id, isPinned)
        }
    }

    fun clearClipboard() {
        viewModelScope.launch {
            repository.clearClipboard()
            _userMessage.value = "Clipboard history cleared"
        }
    }

    fun seedFlagshipDemo() {
        viewModelScope.launch {
            repository.seedSampleFlagships()
            _userMessage.value = "Sample flagships loaded!"
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearDatabase()
            _userMessage.value = "Database cleared"
        }
    }

    fun updateColumnConfig(brandIdx: Int, modelIdx: Int, fullNameIdx: Int) {
        _columnConfig.value = SmartphoneSpecDef.ColumnMappingConfig(
            brandColumnIndex = brandIdx,
            modelColumnIndex = modelIdx,
            fullNameColumnIndex = fullNameIdx
        )
        _userMessage.value = "Column mapping updated"
    }

    suspend fun exportJson(): String = repository.exportToJson()
    suspend fun exportTsv(): String = repository.exportToTsv()
    suspend fun exportCsv(): String = repository.exportToCsv()
    suspend fun importJson(json: String): Int = repository.importFromJson(json)
}
