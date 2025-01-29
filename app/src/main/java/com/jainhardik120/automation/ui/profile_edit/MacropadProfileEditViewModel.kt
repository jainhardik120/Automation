package com.jainhardik120.automation.ui.profile_edit

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.jainhardik120.automation.data.database.MacropadDatabase
import com.jainhardik120.automation.data.database.entities.MacropadProfile
import com.jainhardik120.automation.ui.AppRoutes
import com.jainhardik120.automation.utils.KeyAction
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MacropadProfileEditViewModel @Inject constructor(
    private val database: MacropadDatabase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var state by mutableStateOf(MacropadProfileEditState())

    companion object {
        private const val TAG = "MacropadProfileEditViewModel"
    }

    init {
        selectProfile()
    }

    fun updateActionAtIndex(action: KeyAction, index: Int) {
        state = state.copy(
            actions = state.actions.toMutableList().apply {
                this[index] = action
            }
        )
    }

    fun insertActionAtEnd() {
        state = state.copy(
            actions = state.actions.toMutableList().apply {
                this.add(KeyAction.Null)
            }
        )
    }

    private fun selectProfile() {
        val id = savedStateHandle.toRoute<AppRoutes.MacroPadProfileEditScreen>().id
        state = state.copy(selectedProfileId = id)
        if (id == -1) {
            state = state.copy(actions = listOf(), strings = listOf(), selectedProfileId = id)
        } else {
            viewModelScope.launch {
                val profile = database.dao.getProfileById(id)
                state = state.copy(
                    actions = profile.actions,
                    strings = profile.strings,
                    name = profile.name
                )
            }
        }
    }

    fun saveProfile() {
        viewModelScope.launch {
            database.dao.upsertProfile(
                MacropadProfile(
                    id = (if (state.selectedProfileId != -1) state.selectedProfileId else 0),
                    actions = state.actions,
                    strings = state.strings,
                    name = state.name
                )
            )
        }
    }
}