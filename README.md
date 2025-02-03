# Llama.cpp SSE API

> *API backend for locally hosted Large Language Models written in Java using llama.cpp*

This is a project that I made to learn more about APIs and LLMs. Due to the project being a learning journey, there are some issues and inefficiencies that exist. If you would like to use a fully functional API that uses OpenAI's system look at [llama-server](https://github.com/ggerganov/llama.cpp) REST API.

## How it works

When a request is made to the API, it loads the JSON conversation and sends it to the model as a *formatted instruct prompt*, it responds in a Server-Sent Event (SSE) stream. It first acknowledges that the model has received the prompt and is working (sent as `event:generating`). To prevent timeouts from the client, pings are sent (configurable - sent as `event:ping`). When the model starts to output text, it sends each token as `data:` as soon as it is generated, this makes the response "live" so the client can see the response while being completed.

You can see an example of the model in action [here](https://chat.coosanta.net) ([source code](https://github.com/Coosanta17/LLM-Frontend)).

***

## How to run
Currently the only way to run it is by cloning the repository and building it. I plan to make a Jar option too in the future (I think jar still works but you have to compile the file and I haven't tested it).

1. Clone the repository
   You can clone it by either using Git or downloading source code

2. Configure the app with your preferred settings. See [Configuration](#configuration).

3. Compile and run. Make sure you have **Java 21 or later** installed. Then run the following command:
   ```bash
   ./mvnw spring-boot:run
   ```
   It may take a few minutes on first compile.

## The Conversation Object
*template*
```json
{
  "uuid": "00000000-0000-0000-0000-000000000000",
  "systemPrompt": "System prompt (can be empty)",
  "name": "Name (can be empty)",
  "messages": [
    {
      "role": "<User | Assistant | System>",
      "content": "Message content"
    }
  ]
}
```

*example:*
```json
{
  "uuid": "123e4567-e89b-12d3-a456-426614174000",
  "systemPrompt": "You are a helpful assistant.",
  "name": "Friendly Conversation",
  "messages": [
    {
      "role": "User",
      "content": "Hello, how are you?"
    },
    {
      "role": "Assistant",
      "content": "I'm good, thank you!"
    }
  ]
}
```

## Configuration
*Default configuration file:*

Find this file at `/projectroot/src/main/resources/application.yml`
```yml
server:
  # Port to run api from
  port: 8080

  forward-headers-strategy: native

spring:
  mvc:
    dispatch-options-request: true
  application:
    name: LLM-API

app:
  # File path where conversations are stored relative to project root.
  conversation-path: ./conversations/

  # Whether to save completion conversations
  # Potentially large and excess conversations in conversations so only use if necessary.
  save-completion-conversations: true

  # Maximum amount of conversations to store in memory.
  # Lower values may increase disk activity but reduce memory usage (small impact)
  loaded-conversations-limit: 100 # TODO: FIX!

  # Whether to ping the client regularly to prevent it from timing out. Especially useful
  # on slow hardware and/or large models and/or large context.
  sse-ping: true

  # Interval in seconds to send ping
  ping-interval: 10


  model:
    # Path to the gguf file relative to project root. Uses placeholder llama.gguf for now.
    path: ./models/llama.gguf

    # How much context the model can store in tokens.
    # Higher number will result in the model being able to
    # remember more of the conversation but will use more RAM.
    # Llama.cpp default 512 (model might be a bit forgetful and will struggle on long responses).
    # Set to 0 to load model default (might use too much ram)
    # Parallel sequences will equally divide context between each other.
    # For example, 1000 context shared among 4 parallel sequences will be 250 context per slot.
    context: 1024

    # How many prompt tokens one batch is. If the prompt is 8 tokens with a batch size of 4 it will
    # process as 2 batches of 4.
    batch-size: 512

    # How many token the model generates each response.
    # It is not recommended to have the number higher than context as this can result
    # in weird responses (endless repetition of the same token)
    # Set to -1 for infinity
    # Set to -2 for until context filled (for some reason this doesn't work for me)
    response-limit: 400

    # How many threads the model uses. The more threads the faster
    # the model responds (not always true, see https://github.com/ggerganov/llama.cpp/blob/master/docs/development/token_generation_performance_tips.md).
    # However, this can result in other applications being impacted due to less resources.
    threads: 4

    # How many concurrent completions the model can do.
    # If there are more than the configured number of parallel sequences it will queue
    # the prompt to be responded to once a spot frees up.
    # The more parallel sequences the model can do the slower it may become on each response
    # If there are multiple responses being generated at once.
    parallel-sequences: 2

    # Set the number of layers to store in VRAM (use default: -1)
    # * You need to compile this yourself if you want GPU offloading.
    # See https://github.com/kherud/java-llama.cpp?tab=readme-ov-file#setup-required
    gpu-layers: -1

    # Time in minutes after last activity before the model unloads from memory.
    # Set to 0 to load the model only when generating.
    # Set to -1 if you want the model to never unload (until program terminates).
    inactivity-timeout: 10

    # Whether to load the model on application startup or not.
    # If false, the model will only load once `/chat` is called from api.
    # Loading on startup will decrease the time the model responds on the
    # first `/chat` the application receives at the cost of more
    # background resource use (especially RAM).
    # Inactivity timeouts still apply, which means if the model doesn't do anything for
    # the configured time since start it will still unload the model.
    # If you want the model to stay loaded for the entirety of the application's
    # lifecycle, set this to true and `inactivity-timeout` to -1.
    load-on-start: false
```
   
## API Routes
### `/api/v1/check` - GET

*to check if api is online*

**expected response:**
```http
200 - OK
```

### `/api/v1/complete` - POST

*sends prompt and returns the model completion as server-sent events*

**parameters:**

- `?type=<string | conversation>` - Whether to process it as a string or conversation. Defaults to `string`

**example requests with cURL:**
```bash
# String Completion (default) 
curl -X POST -H "Content-Type: application/json" -d '"Hello world!"' "http://localhost:8080/api/v1/complete"

# String Completion: 
curl -X POST -H "Content-Type: application/json" -d '"Hello world!"' "http://localhost:8080/api/v1/complete?type=string"

# Conversation: 
curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"You are a helpful assistant","messages":[{"role":"Assistant","content":"How can I help you today?"}, {"role":"User", "content":"Hello!"}]}' "http://localhost:8080/api/v1/complete?type=conversation"
```

**example response (streamed):**
```http
event:generating

event:ping

event:ping

data:Hello

data:!

event:ping

data: How

data: are

data: you

data:?

data:
data:
```

### `/api/v1/completion-title` - POST
*Generates a title based on input conversation*

**Example request:**
```bash
 curl -X POST -H "Content-Type: application/json" -d '{"systemPrompt":"your a youg chil wh canr speel","messages":[{"role":"User","content":"Hello!"}, {"role":"Assistant","content":"Hi! How may I help you!"}, {"role":"User","content":"Tell me about black holes"}, {"role":"Assistant","content":"Informati abog blak wholes can be foun on internet lol"}]}' "http://localhost:8080/api/v1/completion-title"
```

**Example response**
```http
event:generating

event:ping

event:ping

event:title
data:Black Hole Investigation
```



That's all for now. If you find any errors anywhere please feel free to create an [issue](https://github.com/Coosanta17/LLM-API/issues/).
