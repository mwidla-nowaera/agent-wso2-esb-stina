# WSO2 ESB mediator for Aino.io


WSO2 ESB mediator implementation of Aino.io logging agent.

## What is [Aino.io](http://aino.io) and what does this Agent have to do with it?

[Aino.io](http://aino.io) is an analytics and monitoring tool for integrated enterprise applications and digital
business processes. Aino.io can help organizations manage, develop, and run the digital parts of their day-to-day
business. Read more from our [web pages](http://aino.io).

Aino.io works by analyzing transactions between enterprise applications and other pieces of software.
This Agent helps to store data about the transactions to Aino.io platform using Aino.io Data API (version 2.0).
See [API documentation](http://www.aino.io/api) for detailed information about the API.

## Technical requirements
* Oracle Java 7/8, Or OpenJDK 8 
* WSO2 ESB (Tested with 4.7.0, 4.8.X, 4.9.0, 5.0.0)
* WSO2 Enterprise Integrator EI 6.x (Tested with 6.5) : Aino.io Agent Ver 1.2.6 

## Example usage
Before using the agent, it must be configured and copied to directory where ESB can find it.
After installing and configuring, the agent can be used with `ainoLog` mediator.

### 1.1 Add as Maven/Gradle/Ivy dependency

Get the dependency snippet from [here](https://ainoio.jfrog.io/ui/repos/tree/General/default-maven-local%2Fio%2Faino%2Fagents%2FAgentWso2Esb).

### 1.2 Install the mediator
Copy the `AgentWso2ESB-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 2. Configuring the agent
As WSO2 ESB agent uses [Java agent](https://github.com/Aino-io/agent-java), it must be configured according
to [Java Agent Configuration](https://github.com/Aino-io/agent-java#configuring-the-agent).
Java agent configuration should be in `$WSO2_ESB_HOME/repository/conf/ainoLogMediatorConfig.xml` (OR `$WSO2_ESB_HOME/conf/ainoLogMediatorConfig.xml` if you are using IE 6.6), where
`$WSO2_ESB_HOME` is your WSO2 ESB installation directory.
NOTE the agent is dependent on ESB axis2 configuration file. `$WSO2_ESB_HOME/repository/conf/axis2/axis2.xml` (OR `$WSO2_ESB_HOME/conf/axis2/axis2.xml` if you are using IE 6.6)

Additionally, one configured 'application' _*must*_ have key `esb`.

### 3. Add Aino.io certificate to WSO2 ESB

##### Export certificate

This instruction is for Chrome browser so if you are using another browser, please google how to export a ssl certificate from a site with your browser.

Open the [site](https://app.aino.io) in the Chrome browser, and then click the small lock icon beside the URL in the address bar. Press `Details` and the developer toolbar opens in the lower part of the browser. Press `View certificate`. In the popup drag from the certificate icon and drop the certificate to the folder `$WSO2_ESB_HOME/repository/resources/security/`. A file named `*.aino.io.cer` was created.

##### Import it to the keystore
Then import the created *.cer file to the keystore. Check the current keystore password from `$WSO2_ESB_HOME/repository/conf/axis2/axis2.xml` (search for parameter called "truststore"). Then run command and give the password:

```
keytool -import -alias aino.io   -file *.aino.io.cer -keystore client-truststore.jks
```

### 4. Send a request to Aino.io:

#### Minimal example (only required fields)
```xml
<ainoLog status="success">
    <to applicationKey="app02" />
</ainoLog>
```

Only `from` or `to` field is allowed, as the mediator automatically sets the other to `esb`.

#### Full example
```xml
<ainoLog status="success">
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

OR ver 1.2.5 onward if you wan't to use dynamic message text 
<ainoLog status="success">
    <operation key="update" />
    <message expression="//order/orderId" />
    <ids expression="//order/orderId" typeKey="dataType01" />
    <ids expression="//order/customerId" typeKey="dataType02" />
    <to applicationKey="app02" />
    <payloadType key="subInterface01" />
    <!-- Property fields can be used to send additional information (showed in metadata section in Aino.io)-->
    <property expression="//someXpath" name="someProp" />
    <property expression="//someOtherXpath" name="someOtherProp" />
    <property expression="//someThirdXpath" name="someThirdProp" />
</ainoLog>

OR ver 1.2.8 onward you can use dynamic to, from and status (NOTE dynamic status atribute is called statusExpression)   AND you can also you both from and to together 
<ainoLog statusExpression="$ctx:statusPropertyValue">
    <operation key="update" />
    <message expression="//order/orderId" />
    <ids expression="//order/orderId" typeKey="dataType01" />
    <ids expression="//order/customerId" typeKey="dataType02" />
    <to expression="//order/toapplication" />
    <from expression="$ctx:fromAppliPropertyValue" />
    <payloadType key="subInterface01" />
    <!-- Property fields can be used to send additional information (showed in metadata section in Aino.io)-->
    <property expression="//someXpath" name="someProp" />
    <property expression="//someOtherXpath" name="someOtherProp" />
    <property expression="//someThirdXpath" name="someThirdProp" />
</ainoLog>

OR ver 1.2.9 onward you can also use dynamic operation  
<ainoLog statusExpression="$ctx:statusPropertyValue">
    <operation expression="//order/operation" />
    <message expression="//order/orderId" />
    <ids expression="//order/orderId" typeKey="dataType01" />
    <ids expression="//order/customerId" typeKey="dataType02" />
    <to expression="//order/toapplication" />
    <from expression="$ctx:fromAppliPropertyValue" />
    <payloadType key="subInterface01" />
    <!-- Property fields can be used to send additional information (showed in metadata section in Aino.io)-->
    <property expression="//someXpath" name="someProp" />
    <property expression="//someOtherXpath" name="someOtherProp" />
    <property expression="//someThirdXpath" name="someThirdProp" />
</ainoLog>

```

All 'keys' must match keys configured in Aino configuration file. Please note that the order of the configuration elements (child elements of ainoLog) must be as specified above (at least for now).

### 5. Enable logging to ESB log (wso2carbon.log)
The sent messages can be logged to ESB's log by setting *INFO* log level for class `io.aino.agents.wso2.mediator.AinoMediator`. 
The log level can be changed via ESB's Management Console by going to Configure -> Logging. 

## Contributing

### Technical requirements
* Oracle Java 7/8, Or OpenJDK 8 
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
