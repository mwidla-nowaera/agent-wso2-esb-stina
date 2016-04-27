# WSO2 ESB mediator for Aino.io
WSO2 ESB mediator implementation of Aino.io logging agent.
This project depends on Aino.io Java Agent.

## What is [Aino.io](http://aino.io) and what does this Agent have to do with it?

[Aino.io](http://aino.io) is an analytics and monitoring tool for integrated enterprise applications and digital
business processes. Aino.io can help organizations manage, develop, and run the digital parts of their day-to-day
business. Read more from our [web pages](http://aino.io).

Aino.io works by analyzing transactions between enterprise applications and other pieces of software.
This Agent helps to store data about the transactions to Aino.io platform using Aino.io Data API (version 2.0).
See [API documentation](http://www.aino.io/api) for detailed information about the API.


## Introduction

## Usage
Before using the agent, it must be configured and copied to directory where ESB can find it.
After installing and configuring, the agent can be used with `ainoLog` mediator.

### 1. Install the mediator
Copy the `AgentWso2ESB-x.y.jar` to `$WSO2_ESB_HOME/repository/components/dropins/`.

### 2. Configuring the agent
As WSO2 ESB agent uses [Java agent](http://url_to_java_agent.com), it must be configured according
to [Java Agent Configuration](http://link.to.conf).
Java agent configuration should be in `$WSO2_ESB_HOME/repository/conf/ainoLogMediatorConf.xml`, where
`$WSO2_ESB_HOME` is your WSO2 ESB installation directory.

Additionally, one configured 'application' _*must*_ have key `esb`.

### 3. Use it

#### Logging to aino.io with only required fields
```xml
<ainoLog monitored="true" status="success">
    <to applicationKey="app02" />
</ainoLog>
```

Only `from` or `to` field is allowed, as the mediator automatically sets the other to `esb`.

#### Logging to aino.io with all fields
```xml
<ainoLog monitored="true" status="success">
    <operation key="update" />
    <message value="success" />
    <ids expression="//order/orderId" typeKey="dataType01" />
    <ids expression="//order/customerId" typeKey="dataType02" />
    <to applicationKey="app02" />
    <payloadType key="subInterface01" />
    <property expression="//someXpath" name="someProp" />
    <property expression="//someOtherXpath" name="someOtherProp" />
    <property expression="//someThirdXpath" name="someThirdProp" />
</ainoLog>
```

All 'keys' must match keys configured in Aino configuration file.

## Two system example

Here we have below a two-system integration example to show how WSO2 ESB mediator for Aino.io can be used. In the example we have WSO2 ESB between CRM-system and Invoicing-system, and the information flow is following: 1) WSO2 ESB receives a message from the CRM 2) WSO2 ESB transforms the message so that it can be sent to Invoicing 3) WSO2 ESB sends it to Invoicing.

### Aino.io configuration file 

Aino.io configuration file used in this example.

```xml
<ainoConfig>
    <ainoLoggerService enabled="true">
        <!-- Aino.io API URL -->
        <address uri="https://data.aino.io/rest/v2.0/transaction"
               apiKey="<your-api-key-here>"/>
        <!-- How of often ESB sends transactions to Aino.io and how many at a time.
             In this example transactions are sent every 10 seconds and in 1000 transaction batches. -->
        <send interval="10000" sizeThreshold="1000"/>
    </ainoLoggerService>
    <!-- Define your operations here --> 
    <operations>
        <operation key="customerDetailsUpdate" name="Customer Details Update"/>
    </operations>
    <!-- To which applications does ESB connect to? -->
    <applications>
        <application key="esb" name="WSO2 ESB"/>
        <application key="crm" name="CRM"/>
        <application key="invoicing" name="Invoicing"/>
    </applications>
    <!-- What kind of information needs to be saved in transactions?-->
    <idTypes>
        <idType key="customerId" name="Customer ID"/>
    </idTypes>
    <!-- What kind of content (in general) is transactions? -->
   <payloadTypes>
      <payloadType key="customerInvoicingAddress" name="Customer Invoicing Address"/>
   </payloadTypes>
</ainoConfig>
```

### Example proxy configuration

This proxy receives the messages from CRM, transforms it and sends it to Invoicing. In case ESB fails to send the request to Invoining, transactions are sent to Aino.io in failure-state.


```xml
<proxy xmlns="http://ws.apache.org/ns/synapse"
       name="FromCrmToInvoicing_Example_Proxy"
       transports="https,http"
       statistics="disable"
       trace="disable"
       startOnLoad="true">
    <target>
        <inSequence>
            <!-- We have received a request from CRM, so lets send transaction info to aino.io about that -->
            <ainoLog monitored="true" status="success">
                <operation key="customerDetailsUpdate"/>
                <message value="success"/>
                <ids expression="//crmRequest/customerId" typeKey="customerId"/>
                <from applicationKey="crm"/>
                <payloadType key="customerInvoicingAddress" />
            </ainoLog>

            <!-- Transform CRM request to the format that Invoicing accepts -->
            <xslt key="gov:/trunk/xslt/CrmRequestToInvoicingRequest.xsl"/>

            <!-- Send the request to Invoicing -->
            <call>
                <endpoint key="invoicingEndpoint"/>
            </call>

            <!-- We have succesfully sent the request to Invoicing, so lets tell that to aino.io -->
            <ainoLog monitored="true" status="success">
                <operation key="customerDetailsUpdate"/>
                <message value="success"/>
                <ids expression="//invoicingResponse/customerId" typeKey="customerId"/>
                <to applicationKey="invoicing"/>
                <payloadType key="customerInvoicingAddress" />
            </ainoLog>
            <drop/>
        </inSequence>
        <faultSequence>
            <!-- Something went wrong when sending the request to Invoicing, so lets send a failure transaction to aino.io -->
            <ainoLog monitored="true" status="failure">
                <operation key="customerDetailsUpdate"/>
                <message value="failure"/>
                <ids expression="//invoicingResponse/customerId" typeKey="customerId"/>
                <to applicationKey="invoicing"/>
                <payloadType key="customerInvoicingAddress" />
            </ainoLog>
            <drop/>
        </faultSequence>
    </target>
</proxy>
```

## Contributors

- [Jarkko Kallio](https://github.com/kallja)
- [Pekka Heino](https://github.com/heinop)
- [Aleksi Mustonen](https://github.com/aleksimustonen)
- [Jussi Mikkonen](https://github.com/jussi-mikkonen)
- [Ville Harvala](https://github.com/vharvala)

## [License](LICENSE)

Copyright &copy; 2016 [Aino.io](http://aino.io). Licensed under the [Apache 2.0 License](LICENSE).
