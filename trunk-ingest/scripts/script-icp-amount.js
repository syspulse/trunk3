var hash = inputs.hash();
var from = inputs.from().isDefined() ? inputs.from().get() : "";
var to = inputs.to().isDefined() ? inputs.to().get() : "";
var value = inputs.v();
var typ = inputs.typ();
//print(">>>",hash);
//hash + ": "+typ +": ("+ from + " -> " + to + " ("+value+"))";
var res = {tx_hash: hash, value: value};

//if(value > 1000000000.0) Java.asJSONCompatible(res); else null;
if(value > 10000.0) Java.asJSONCompatible(res); else null;