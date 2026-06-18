package com.example.smstester

import app.cash.turbine.test
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.truth.Truth.assertThat

/**
 * Sample ViewModel test demonstrating Turbine and Flow testing patterns.
 * 
 * This test shows:
 * - Setting up test dispatcher
 * - Mocking dependencies
 * - Testing state emissions with Turbine
 * - Testing user actions
 * - Testing error handling
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SampleViewModelTest {
    
    private val testDispatcher = UnconfinedTestDispatcher()
    
    // Mocks
    private val repository: SmsRepository = mockk()
    private val deleteUseCase: DeleteMessagesUseCase = mockk()
    
    // ViewModel under test
    private lateinit var viewModel: MessagesViewModel
    
    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MessagesViewModel(repository, deleteUseCase)
    }
    
    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }
    
    @Test
    fun 	est initial state is loading() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isTrue()
            assertThat(state.messages).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun 	est messages loaded successfully() = runTest {
        // Arrange
        val expectedMessages = listOf(
            SmsMessage(id = "1", body = "Hello", address = "1234567890", date = System.currentTimeMillis()),
            SmsMessage(id = "2", body = "World", address = "0987654321", date = System.currentTimeMillis() - 1000)
        )
        coEvery { repository.getAllMessages() } returns flowOf(expectedMessages)
        
        // Act
        viewModel.loadMessages()
        
        // Assert
        viewModel.uiState.test {
            // Skip loading state
            awaitItem()
            
            val loadedState = awaitItem()
            assertThat(loadedState.isLoading).isFalse()
            assertThat(loadedState.messages).hasSize(2)
            assertThat(loadedState.messages[0].body).isEqualTo("Hello")
            assertThat(loadedState.messages[1].body).isEqualTo("World")
            
            cancelAndIgnoreRemainingEvents()
        }
        
        coVerify(exactly = 1) { repository.getAllMessages() }
    }
    
    @Test
    fun 	est delete message updates state() = runTest {
        // Arrange
        val messageId = "1"
        val remainingMessages = listOf(
            SmsMessage(id = "2", body = "World", address = "0987654321", date = System.currentTimeMillis())
        )
        
        coEvery { deleteUseCase(messageId) } just Runs
        coEvery { repository.getAllMessages() } returns flowOf(remainingMessages)
        
        // Act
        viewModel.deleteMessage(messageId)
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.messages).hasSize(1)
            assertThat(state.messages[0].body).isEqualTo("World")
            cancelAndIgnoreRemainingEvents()
        }
        
        coVerify(exactly = 1) { deleteUseCase(messageId) }
    }
    
    @Test
    fun 	est error handling shows error state() = runTest {
        // Arrange
        val errorMessage = "Failed to load messages"
        coEvery { repository.getAllMessages() } throws Exception(errorMessage)
        
        // Act
        viewModel.loadMessages()
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.isLoading).isFalse()
            assertThat(state.error).isEqualTo(errorMessage)
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun 	est search filters messages() = runTest {
        // Arrange
        val allMessages = listOf(
            SmsMessage(id = "1", body = "Meeting at 3pm", address = "123", date = System.currentTimeMillis()),
            SmsMessage(id = "2", body = "Lunch at noon", address = "456", date = System.currentTimeMillis())
        )
        coEvery { repository.getAllMessages() } returns flowOf(allMessages)
        
        // Act
        viewModel.loadMessages()
        viewModel.onSearchQueryChange("Meeting")
        
        // Assert
        viewModel.uiState.test {
            val state = awaitItem()
            assertThat(state.filteredMessages).hasSize(1)
            assertThat(state.filteredMessages[0].body).isEqualTo("Meeting at 3pm")
            cancelAndIgnoreRemainingEvents()
        }
    }
    
    @Test
    fun 	est refresh reloads data() = runTest {
        // Arrange
        val initialMessages = listOf(
            SmsMessage(id = "1", body = "Initial", address = "123", date = System.currentTimeMillis())
        )
        val refreshedMessages = listOf(
            SmsMessage(id = "1", body = "Initial", address = "123", date = System.currentTimeMillis()),
            SmsMessage(id = "2", body = "New", address = "456", date = System.currentTimeMillis())
        )
        
        coEvery { repository.getAllMessages() } returnsMany listOf(
            flowOf(initialMessages),
            flowOf(refreshedMessages)
        )
        
        // Act
        viewModel.loadMessages()
        viewModel.refresh()
        
        // Assert
        viewModel.uiState.test {
            // First load
            awaitItem() // Loading
            val initialState = awaitItem()
            assertThat(initialState.messages).hasSize(1)
            
            // Refresh
            awaitItem() // Loading
            val refreshedState = awaitItem()
            assertThat(refreshedState.messages).hasSize(2)
            
            cancelAndIgnoreRemainingEvents()
        }
    }
}

/**
 * Sample Compose UI test demonstrating testing patterns.
 */
class SampleComposeTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun 	est message list displays messages() {
        // Arrange
        val messages = listOf(
            SmsMessage(id = "1", body = "Hello", address = "123", date = System.currentTimeMillis()),
            SmsMessage(id = "2", body = "World", address = "456", date = System.currentTimeMillis())
        )
        
        composeTestRule.setContent {
            MessageList(messages = messages)
        }
        
        // Assert
        composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
        composeTestRule.onNodeWithText("World").assertIsDisplayed()
        composeTestRule.onAllNodesWithTag("message_item").assertCountEquals(2)
    }
    
    @Test
    fun 	est delete button removes message() {
        // Arrange
        var deletedId: String? = null
        val messages = listOf(
            SmsMessage(id = "1", body = "Test", address = "123", date = System.currentTimeMillis())
        )
        
        composeTestRule.setContent {
            MessageList(
                messages = messages,
                onDelete = { deletedId = it }
            )
        }
        
        // Act
        composeTestRule.onNodeWithContentDescription("Delete").performClick()
        
        // Assert
        assertThat(deletedId).isEqualTo("1")
    }
    
    @Test
    fun 	est search field filters results() {
        // Arrange
        composeTestRule.setContent {
            SearchScreen()
        }
        
        // Act
        composeTestRule.onNodeWithTag("search_field").performTextInput("Meeting")
        
        // Assert
        composeTestRule.onNodeWithText("Meeting at 3pm").assertIsDisplayed()
        composeTestRule.onNodeWithText("Lunch at noon").assertDoesNotExist()
    }
    
    @Test
    fun 	est loading state shows progress indicator() {
        // Arrange
        composeTestRule.setContent {
            MessagesScreen(isLoading = true)
        }
        
        // Assert
        composeTestRule.onNodeWithTag("loading_indicator").assertIsDisplayed()
        composeTestRule.onNodeWithText("No messages").assertDoesNotExist()
    }
    
    @Test
    fun 	est empty state shows message() {
        // Arrange
        composeTestRule.setContent {
            MessagesScreen(messages = emptyList())
        }
        
        // Assert
        composeTestRule.onNodeWithText("No messages").assertIsDisplayed()
    }
}

/**
 * Sample Repository test demonstrating ContentResolver mocking.
 */
class SampleRepositoryTest {
    
    private val contentResolver: ContentResolver = mockk(relaxed = true)
    private lateinit var repository: SmsRepository
    
    @Before
    fun setup() {
        repository = SmsRepository(contentResolver)
    }
    
    @Test
    fun 	est get all messages returns list() = runTest {
        // Arrange
        val cursor = mockk<Cursor>()
        
        every { 
            contentResolver.query(
                Telephony.Sms.CONTENT_URI, any(), any(), any(), any()
            )
        } returns cursor
        
        every { cursor.moveToFirst() } returns true
        every { cursor.moveToNext() } returnsMany listOf(true, false)
        every { cursor.getColumnIndex(Telephony.Sms._ID) } returns 0
        every { cursor.getColumnIndex(Telephony.Sms.BODY) } returns 1
        every { cursor.getColumnIndex(Telephony.Sms.ADDRESS) } returns 2
        every { cursor.getColumnIndex(Telephony.Sms.DATE) } returns 3
        every { cursor.getString(0) } returnsMany listOf("1", "2")
        every { cursor.getString(1) } returnsMany listOf("Hello", "World")
        every { cursor.getString(2) } returnsMany listOf("123", "456")
        every { cursor.getLong(3) } returnsMany listOf(
            System.currentTimeMillis(), 
            System.currentTimeMillis() - 1000
        )
        
        // Act
        val result = repository.getAllMessages()
        
        // Assert
        assertThat(result).hasSize(2)
        assertThat(result[0].body).isEqualTo("Hello")
        assertThat(result[1].body).isEqualTo("World")
        
        verify { cursor.close() }
    }
    
    @Test
    fun 	est delete message returns true on success() = runTest {
        // Arrange
        val messageId = "123"
        val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId)
        every { contentResolver.delete(uri, null, null) } returns 1
        
        // Act
        val result = repository.deleteMessage(messageId)
        
        // Assert
        assertThat(result).isTrue()
        verify(exactly = 1) { contentResolver.delete(uri, null, null) }
    }
    
    @Test
    fun 	est delete message returns false on failure() = runTest {
        // Arrange
        val messageId = "123"
        val uri = Uri.withAppendedPath(Telephony.Sms.CONTENT_URI, messageId)
        every { contentResolver.delete(uri, null, null) } returns 0
        
        // Act
        val result = repository.deleteMessage(messageId)
        
        // Assert
        assertThat(result).isFalse()
    }
}

/**
 * Sample UseCase test demonstrating testing patterns.
 */
class SampleUseCaseTest {
    
    private val repository: SmsRepository = mockk()
    private lateinit var useCase: DeleteOldMessagesUseCase
    
    @Before
    fun setup() {
        useCase = DeleteOldMessagesUseCase(repository)
    }
    
    @Test
    fun 	est delete messages older than threshold() = runTest {
        // Arrange
        val thresholdDays = 30
        val thresholdTime = System.currentTimeMillis() - (thresholdDays * 24 * 60 * 60 * 1000L)
        
        val oldMessages = listOf(
            SmsMessage(id = "1", body = "Old", address = "123", date = thresholdTime - 1000),
            SmsMessage(id = "2", body = "Older", address = "456", date = thresholdTime - 2000)
        )
        
        coEvery { repository.getMessagesOlderThan(thresholdDays) } returns oldMessages
        coEvery { repository.bulkDeleteMessages(any()) } returns 2
        
        // Act
        val result = useCase(DeleteOldMessagesUseCase.Params(days = thresholdDays))
        
        // Assert
        assertThat(result.deletedCount).isEqualTo(2)
        coVerify(exactly = 1) { repository.bulkDeleteMessages(listOf("1", "2")) }
    }
    
    @Test
    fun 	est no messages to delete returns zero() = runTest {
        // Arrange
        coEvery { repository.getMessagesOlderThan(any()) } returns emptyList()
        
        // Act
        val result = useCase(DeleteOldMessagesUseCase.Params(days = 30))
        
        // Assert
        assertThat(result.deletedCount).isEqualTo(0)
        coVerify(exactly = 0) { repository.bulkDeleteMessages(any()) }
    }
    
    @Test
    fun 	est repository error propagates() = runTest {
        // Arrange
        val exception = Exception("Database error")
        coEvery { repository.getMessagesOlderThan(any()) } throws exception
        
        // Act & Assert
        try {
            useCase(DeleteOldMessagesUseCase.Params(days = 30))
            assertThat(false).isTrue() // Should not reach here
        } catch (e: Exception) {
            assertThat(e.message).isEqualTo("Database error")
        }
    }
}
