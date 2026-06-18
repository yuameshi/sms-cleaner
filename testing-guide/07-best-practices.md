# 7. Best Practices

## Test Organization

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

## Test Naming Convention

`kotlin
// Pattern: test [action] [expected result]
@Test
fun 	est delete message with valid id returns success() { }

@Test
fun 	est get messages with empty database returns empty list() { }

@Test
fun 	est search with no results shows empty state() { }
`

## Common Testing Patterns

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

## Performance Testing

`kotlin
@Test
fun testPerformance() = runTest {
    val iterations = 1000
    val startTime = System.currentTimeMillis()
    
    repeat(iterations) {
        useCase(GetMessagesUseCase.Params())
    }
    
    val endTime = System.currentTimeMillis()
    val duration = endTime - startTime
    
    assertThat(duration).isLessThan(5000) // 5 seconds
}
`

## Testing Edge Cases

`kotlin
@Test
fun 	est with null values() = runTest {
    coEvery { repository.getMessages(any(), any()) } returns listOf(
        SmsMessage(id = "1", body = null, address = null, date = 0L)
    )
    
    val result = useCase(GetMessagesUseCase.Params())
    
    assertThat(result).hasSize(1)
    assertThat(result[0].body).isNull()
}

@Test
fun 	est with empty strings() = runTest {
    coEvery { repository.getMessages(any(), any()) } returns listOf(
        SmsMessage(id = "", body = "", address = "", date = 0L)
    )
    
    val result = useCase(GetMessagesUseCase.Params())
    
    assertThat(result).hasSize(1)
    assertThat(result[0].body).isEmpty()
}

@Test
fun 	est with very large dataset() = runTest {
    val largeDataset = (1..10000).map { 
        SmsMessage(id = it.toString(), body = "Message ", address = "123", 
                  date = System.currentTimeMillis())
    }
    coEvery { repository.getMessages(any(), any()) } returns largeDataset
    
    val result = useCase(GetMessagesUseCase.Params())
    
    assertThat(result).hasSize(10000)
}
`

## TDD Workflow

1. **Red**: Write a failing test
2. **Green**: Write minimal code to pass
3. **Refactor**: Improve code while keeping tests green

`kotlin
// Step 1: Write failing test
@Test
fun 	est delete old messages() = runTest {
    val useCase = DeleteOldMessagesUseCase(repository)
    val result = useCase(Params(days = 30))
    assertThat(result.deletedCount).isEqualTo(2)
}

// Step 2: Implement UseCase
class DeleteOldMessagesUseCase(
    private val repository: SmsRepository
) : UseCase<Params, Result> {
    override suspend fun invoke(params: Params): Result {
        val oldMessages = repository.getMessagesOlderThan(params.days)
        val deletedCount = repository.bulkDeleteMessages(oldMessages.map { it.id })
        return Result(deletedCount)
    }
}

// Step 3: Refactor if needed
`

## Resources

- [Android Testing Documentation](https://developer.android.com/training/testing)
- [Jetpack Compose Testing](https://developer.android.com/jetpack/compose/testing)
- [Hilt Testing](https://dagger.dev/hilt/testing.html)
- [Turbine Library](https://github.com/cashapp/turbine)
- [MockK Documentation](https://mockk.io/)
- [Truth Assertions](https://truth.dev/)
