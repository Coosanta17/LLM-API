You can place your gguf llm models here. Download some from [huggingface](https://huggingface.co/).

You can specify a custom place to load models from in the `application.yml` configuration file by providing its location with the `--spring.config.location` argument

*<u>Example</u>:*
```bash
java -jar LLM-API.jar --spring.config.location=/path/to/application.yml
```
(Learn more [here](https://github.com/Coosanta17/LLM-API#Config)) <-- fix!!