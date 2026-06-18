# Android TDD and Testing Patterns - Complete Guide

## Overview

This guide provides comprehensive testing patterns for Android apps with Jetpack Compose, covering:
- **Compose UI Testing**: Testing UI components and user interactions
- **ViewModel Testing**: Testing state management with Turbine/Flow
- **Repository Testing**: Testing data access with mocked ContentResolver
- **UseCase Testing**: Testing business logic patterns
- **Hilt Testing**: Dependency injection testing setup
- **Integration Testing**: End-to-end SMS operation testing

## Quick Start

### 1. Add Dependencies

See [build-config.md](build-config.md) for complete dependency setup.

### 2. Create Test Structure

`
app/
├── src/
│   ├── main/
│   ├── test/           # Unit tests (fast, no device)
│   │   └── java/com/example/
│   │       ├── viewmodel/
│   │       ├── repository/
│   │       ├── usecase/
│   │       └── model/
│   └── androidTest/    # Instrumented tests (requires device)
│       └── java/com/example/
│           ├── ui/
│           ├── integration/
│           └── hilt/
`

### 3. Write Your First Test

`kotlin
@Test
fun 	est delete message with valid id returns success() = runTest {
    // Arrange
    val messageId = "123"
    coEvery { repository.deleteMessage(messageId) } returns true
    
    // Act
    val result = useCase(DeleteMessageUseCase.Params(messageId))
    
    // Assert
    assertThat(result.isSuccess).isTrue()
    coVerify(exactly = 1) { repository.deleteMessage(messageId) }
}
`

## Key Testing Patterns

### Compose UI Testing

`kotlin
@get:Rule
val composeTestRule = createComposeRule()

@Test
fun testUI() {
    composeTestRule.setContent {
        MyScreen()
    }
    
    composeTestRule.onNodeWithText("Hello").assertIsDisplayed()
    composeTestRule.onNodeWithText("Button").performClick()
    composeTestRule.onNodeWithText("Updated").assertIsDisplayed()
}
`

### ViewModel Testing with Turbine

`kotlin
@Test
fun testViewModel() = runTest {
    coEvery { repository.getData() } returns flowOf(listOf("Item"))
    
    viewModel.uiState.test {
        val state = awaitItem()
        assertThat(state.data).hasSize(1)
        cancelAndIgnoreRemainingEvents()
    }
}
`

### Repository Testing with Mocked ContentResolver

`kotlin
@Test
fun testRepository() = runTest {
    val cursor = mockk<Cursor>()
    every { contentResolver.query(any(), any(), any(), any(), any()) } returns cursor
    every { cursor.moveToFirst() } returns true
    // ... setup cursor behavior
    
    val result = repository.getAllMessages()
    
    assertThat(result).hasSize(2)
    verify { cursor.close() }
}
`

### Hilt Testing Setup

`kotlin
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class MyTest {
    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)
    
    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()
    
    @Inject
    lateinit var repository: SmsRepository
    
    @Before
    fun setup() {
        hiltRule.inject()
    }
}
`

## Test Naming Convention

`kotlin
@Test
fun 	est [action] [expected result]() { }

// Examples:
@Test
fun 	est delete message with valid id returns success() { }

@Test
fun 	est get messages with empty database returns empty list() { }

@Test
fun 	est search with no results shows empty state() { }
`

## Common Patterns

### Arrange-Act-Assert (AAA)

`kotlin
@Test
fun testSomething() {
    // Arrange - Setup test data and mocks
    val expectedData = listOf("Item 1", "Item 2")
    coEvery { repository.getData() } returns flowOf(expectedData)
    
    // Act - Execute the action
    val result = viewModel.loadData()
    
    // Assert - Verify the result
    assertThat(result).isEqualTo(expectedData)
}
`

### Given-When-Then (BDD)

`kotlin
@Test
fun testSomething() {
    // Given - Initial state
    coEvery { repository.getData() } returns flowOf(listOf("Item"))
    
    // When - Action performed
    viewModel.loadData()
    
    // Then - Expected outcome
    viewModel.uiState.test {
        val state = awaitItem()
        assertThat(state.data).hasSize(1)
        cancelAndIgnoreRemainingEvents()
    }
}
`

## Test Doubles

| Type | Description | Use When |
|------|-------------|----------|
| **Fake** | Working implementation with simplified logic | Testing real behavior without external dependencies |
| **Mock** | Pre-programmed with expectations | Verifying interactions |
| **Stub** | Returns fixed data | Providing specific test data |
| **Spy** | Records calls for verification | Checking method calls |

## Resources

- [Android Testing Documentation](https://developer.android.com/training/testing)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Hilt Testing](https://dagger.dev/hilt/testing.html)
- [Turbine Library](https://github.com/cashapp/turbine)
- [MockK Documentation](https://mockk.io/)
- [Truth Assertions](https://truth.dev/)

## Files in This Guide

1. [README.md](README.md) - Overview and quick reference
2. [01-compose-testing.md](01-compose-testing.md) - Compose UI testing patterns
3. [02-viewmodel-testing.md](02-viewmodel-testing.md) - ViewModel testing with Turbine
4. [03-repository-testing.md](03-repository-testing.md) - Repository testing with ContentResolver
5. [04-usecase-testing.md](04-usecase-testing.md) - UseCase testing patterns
6. [05-hilt-testing.md](05-hilt-testing.md) - Hilt testing setup
7. [06-integration-testing.md](06-integration-testing.md) - Integration testing for SMS
8. [07-best-practices.md](07-best-practices.md) - Best practices and patterns
9. [build-config.md](build-config.md) - Build configuration and dependencies
10. [sample-tests.kt](sample-tests.kt) - Sample test file with examples
11. [QUICK-REFERENCE.md](QUICK-REFERENCE.md) - Quick reference card
