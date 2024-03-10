var scalaOption = Java.type('scala.Option');
var hash = inputs.hash();
var from = inputs.from().isDefined() ? inputs.from().get() : "";
var to = inputs.to().isDefined() ? inputs.to().get() : "";
var value = inputs.v();
var typ = inputs.typ();
//print(">>>",hash);
hash + ": "+typ +"("+ from + " -> " + to + " ("+value+"))";