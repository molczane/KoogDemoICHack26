# Planner Agents

Planner agents plan and execute multi-step tasks through iterative planning cycles. They build/update plans, execute steps, and check goal achievement.

## Dependencies

```kotlin
dependencies {
    implementation("ai.koog:koog-agents:$koog_version")
    implementation("ai.koog.agents:agents-planner:$koog_version")
    implementation("io.ktor:ktor-client-cio:$ktor_version")
}
```

## Planning Cycle

1. Build a plan based on current state
2. Execute a single step, updating state
3. Check if goal is achieved
4. Repeat if goal not met

## Simple LLM-Based Planner

```kotlin
val planner = SimpleLLMPlanner()
val strategy = AIAgentPlannerStrategy(name = "simple-planner", planner = planner)
val agentConfig = AIAgentConfig(
    prompt = prompt("planner") { system("You are a helpful planning assistant.") },
    model = OpenAIModels.Chat.GPT4o,
    maxAgentIterations = 50
)
val agent = PlannerAIAgent(
    promptExecutor = simpleOpenAIExecutor(System.getenv("OPENAI_API_KEY")),
    strategy = strategy,
    agentConfig = agentConfig
)
val result = agent.run("Create a plan to organize a team meeting")
```

## GOAP (Goal-Oriented Action Planning)

GOAP uses A* search to find optimal action sequences based on predefined goals and actions.

### Key Concepts

- **State**: Current world representation
- **Actions**: Operations with preconditions, effects, costs, and execution logic
- **Goals**: Target conditions with heuristic costs

### Creating GOAP Agents

```kotlin
data class ContentState(
    val topic: String,
    val hasOutline: Boolean = false,
    val outline: String = "",
    val hasDraft: Boolean = false,
    val draft: String = "",
    val hasReview: Boolean = false,
    val isPublished: Boolean = false
)

val planner = goap<ContentState>(typeOf<ContentState>()) {
    action(
        name = "Create outline",
        precondition = { state -> !state.hasOutline },
        belief = { state -> state.copy(hasOutline = true, outline = "Outline") },
        cost = { 1.0 }
    ) { ctx, state ->
        val response = ctx.llm.writeSession {
            appendPrompt {
                user("Create a detailed outline for: ${state.topic}")
            }
            requestLLM()
        }
        state.copy(hasOutline = true, outline = response.content)
    }

    action(
        name = "Write draft",
        precondition = { state -> state.hasOutline && !state.hasDraft },
        belief = { state -> state.copy(hasDraft = true) },
        cost = { 2.0 }
    ) { ctx, state ->
        // Draft writing logic
        state.copy(hasDraft = true, draft = "Draft content")
    }

    goal(
        name = "Published article",
        description = "Complete and publish the article",
        condition = { state -> state.isPublished }
    )
}
```

### Custom Cost Functions

```kotlin
action(
    name = "Expensive operation",
    precondition = { true },
    belief = { state -> state.copy(operationDone = true) },
    cost = { state ->
        if (state.hasOptimization) 1.0 else 10.0
    }
) { ctx, state -> state.copy(operationDone = true) }
```

### State Beliefs vs Actual Execution

GOAP distinguishes between:
- **Beliefs**: Optimistic predictions for planning
- **Actual execution**: Real state updates that may differ from predictions
