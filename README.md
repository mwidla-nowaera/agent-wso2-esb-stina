# WSO2 ESB mediator for Aino.io

![Build status](https://circleci.com/gh/Aino-io/agent-wso2-esb.svg?style=shield&circle-token=71ead89bc64357e87013b71a2e5bf740d1e7fdbb)

WSO2 ESB mediator implementation of Aino.io logging agent.
This project depends on Aino.io Java Agent.

## What is [Aino.io](http://aino.io) and what does this Agent have to do with it?

[Aino.io](http://aino.io) is an analytics and monitoring tool for integrated enterprise applications and digital
business processes. Aino.io can help organizations manage, develop, and run the digital parts of their day-to-day
business. Read more from our [web pages](http://aino.io).

Aino.io works by analyzing transactions between enterprise applications and other pieces of software.
This Agent helps to store data about the transactions to Aino.io platform using Aino.io Data API (version 2.0).
See [API documentation](http://www.aino.io/api) for detailed information about the API.

## Technical requirements
* Oracle Java 6 & 7
* WSO2 ESB (Tested with ESB 4.7.0 and 4.8.X)

## Example usage
Before using the agent, it must be configured and copied to directory where ESB can find it.
After installing and configuring, the agent can be used with `ainoLog` mediator.

### 1.1 Add as Maven/Gradle/Ivy dependency

Get the dependency snippet from [here](https://bintray.com/aino-io/maven/agent-wso2-esb/view).

Notice that WSO2 Agent depends on the [Java agent](https://github.com/Aino-io/agent-java) so also include that in your depedency list.

### 1.2 Install the mediator
Copy the `AgentWso2ESB-x.y.jar` and `AgentJava-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 2. Configuring the agent
As WSO2 ESB agent uses [Java agent](https://github.com/Aino-io/agent-java), it must be configured according
to [Java Agent Configuration](https://github.com/Aino-io/agent-java#configuring-the-agent).
Java agent configuration should be in `$WSO2_ESB_HOME/repository/conf/ainoLogMediatorConf.xml`, where
`$WSO2_ESB_HOME` is your WSO2 ESB installation directory.

Additionally, one configured 'application' _*must*_ have key `esb`.

### 3. Send a request to Aino.io:

#### Minimal example (only required fields)
```xml
<ainoLog monitored="true" status="success">
    <to applicationKey="app02" />
</ainoLog>
```

Only `from` or `to` field is allowed, as the mediator automatically sets the other to `esb`.

#### Full example
```xml
<ainoLog monitored="true" status="success">
    <operation key="update" />
    <message value="success" />
    <ids expression="//order/orderId" typeKey="dataType01" />
    <ids expression="//order/customerId" typeKey="dataType02" />
    <to applicationKey="app02" />
    <payloadType key="subInterface01" />
    <!-- Property fields can be used to send additional information (showed in metadata section in Aino.io)-->
    <property expression="//someXpath" name="someProp" />
    <property expression="//someOtherXpath" name="someOtherProp" />
    <property expression="//someThirdXpath" name="someThirdProp" />
</ainoLog>
```

All 'keys' must match keys configured in Aino configuration file.

## Contributing

### Technical requirements
* Java Oracle 6
* Maven 3.X
* WSO2 ESB

### Contributors

- [Jarkko Kallio](https://github.com/kallja)
- [Pekka Heino](https://github.com/heinop)
- [Aleksi Mustonen](https://github.com/aleksimustonen)
- [Jussi Mikkonen](https://github.com/jussi-mikkonen)
- [Ville Harvala](https://github.com/vharvala)

## [License](LICENSE)

Copyright &copy; 2016 [Aino.io](http://aino.io). Licensed under the [Apache 2.0 License](LICENSE).
