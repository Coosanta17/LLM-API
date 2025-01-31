# Llama.cpp SSE API (bad)

> *API backend for locally hosted Large Language Models written in Java using llama.cpp*

This is a project that I made to learn more about APIs and LLMs. Due to the project being a learning journey, there are some issues and inefficiencies that exist. If you would like to use a fully functional API that uses OpenAI's system look at [llama-server](https://github.com/ggerganov/llama.cpp) REST API.

## How it works

When a request is made to the API, it loads the JSON conversation and sends it to the model as a *formatted instruct prompt*, it responds in a Server-Sent Event (SSE) stream. It first acknowledges that the model has received the prompt and is working (sent as `event:generating`). To prevent timeouts from the client, pings are sent (configurable - sent as `event:ping`). When the model starts to output text, it sends each token as `data:` as soon as it is generated, this makes the response "live" so the client can see the response while being completed.

You can see an example of the model in action [here](https://chat.coosanta.net) ([source code](https://github.com/Coosanta17/LLM-Frontend)).

***

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

## API Routes
### GET - `/api/v1/check`

*to check if api is online*

**expected response:**
```http
200 - OK
```

### POST - `/api/v1/complete`

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

### POST - `/api/v1/completion-title`
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