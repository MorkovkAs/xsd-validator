# XML Validator Against XSD Schema

It is a simple tool for validating xml files and printing a full list of errors, a list of grouped errors.

##Installation

Clone the source locally:
```
$ git clone https://github.com/MorkovkAs/xsd-validator/
```
1. Build, install.
2. Or use current .jar file in `out/artifacts/xsd_validator_jar/xsd-validator.jar`

##Params
##### pathForXsd

*Required*\
Type: `String`

Absolute path to xsd scheme

##### pathForXml

*Required*\
Type: `String`

Absolute path to validated xml file or package of files 

##### isActiveFullErrorLogging

Type: `Boolean` \
Default: `true`

Flag to enable logging of all errors for each file

##### isActiveValidFilesCopying

Type: `Boolean` \
Default: `false`

Flag to enable copying valid files to `valid` directory

##Usage examples
```
java -jar xsd-validator.jar path_to_xsd path_to_xml_files
java -jar xsd-validator.jar path_to_xsd path_to_xml_files false
java -jar xsd-validator.jar path_to_xsd path_to_xml_files false true
```

##Thanks!
Any questions or problem give me a shout on email avklimakov@gmail.com

##License
Copyright 2020 Anton Klimakov\
Licensed under the Apache License, Version 2.0