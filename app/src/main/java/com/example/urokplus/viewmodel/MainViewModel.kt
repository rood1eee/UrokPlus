package com.example.urokplus.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.urokplus.*
import com.example.urokplus.util.Resource
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MainViewModel(private val repository: AuthRepository) : ViewModel() {

    private val _profile = MutableStateFlow<UserProfile>(UserProfile())
    val profile: StateFlow<UserProfile> = _profile.asStateFlow()

    private val _gradeEvents = MutableStateFlow<Resource<List<GradeEvent>>>(Resource.Loading())
    val gradeEvents: StateFlow<Resource<List<GradeEvent>>> = _gradeEvents.asStateFlow()

    private val _assignments = MutableStateFlow<Resource<List<Assignment>>>(Resource.Loading())
    val assignments: StateFlow<Resource<List<Assignment>>> = _assignments.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, Resource<List<Message>>>>(emptyMap())
    val chatMessages: StateFlow<Map<String, Resource<List<Message>>>> = _chatMessages.asStateFlow()

    init {
        loadProfile()
        startPollingGrades()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _profile.value = repository.getProfile()
        }
    }

    private fun startPollingGrades() {
        viewModelScope.launch {
            while (true) {
                _gradeEvents.value = repository.getGradeEvents()
                delay(10000)
            }
        }
    }

    fun loadAssignments(date: LocalDate, gradeClass: String) {
        viewModelScope.launch {
            _assignments.value = Resource.Loading()
            _assignments.value = repository.getAssignments(date, gradeClass)
        }
    }

    fun startChatPolling(chatId: String) {
        viewModelScope.launch {
            while (true) {
                val currentMessages = repository.getMessages(chatId)
                _chatMessages.value = _chatMessages.value + (chatId to currentMessages)
                delay(3000)
            }
        }
    }

    fun sendMessage(chatId: String, text: String, type: MessageType) {
        viewModelScope.launch {
            val msg = Message(chatId = chatId, text = text, isMe = true, type = type)
            repository.sendMessage(msg)
            // Обновляем сразу для отзывчивости
            _chatMessages.value[chatId]?.data?.let { list ->
                _chatMessages.value = _chatMessages.value + (chatId to Resource.Success(list + msg))
            }
        }
    }
    
    fun saveProfile(p: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(p)
            _profile.value = p
        }
    }
}
