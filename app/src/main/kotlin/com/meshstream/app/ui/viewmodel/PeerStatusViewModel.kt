package com.meshstream.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.meshstream.core.model.NodeInfo
import com.meshstream.core.repository.NodeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * ViewModel for [PeerStatusScreen].
 *
 * Exposes the live list of discovered peers from [NodeRepository].
 */
@HiltViewModel
class PeerStatusViewModel @Inject constructor(
    nodeRepository: NodeRepository,
) : ViewModel() {

    val peers: StateFlow<List<NodeInfo>> = nodeRepository
        .observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )
}
