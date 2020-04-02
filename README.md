# XQuery-Processor
XQuery-Processor is a project for CSE232B at UCSD. It parses XQuery using Antlr and extracts corresponding data from XML documents.

## Build
Use Maven to compile the project by executing the following command:

mvn compile

## Usage
Write XQuery in /test/XQueryTest.txt, and put XML documents in /test. Run the program, the result will be saved in /test/result.txt.

For example, run the following XQuery:

```
<acts> { for $a in doc("j_caesar.xml")//ACT

         where empty ( for $sp in $a/SCENE/SPEECH/SPEAKER
           
                       where $sp/text() = "CASCA" 
                            
                       return <speaker> {$sp/text()}</speaker> )
                            
         return <act>{$a/TITLE/text()}</act>
           
}</acts>
```

the result will be:

`<acts>`

    <act>ACT IV</act>
  
    <act>ACT V</act>
  
`</acts>`
