var hash = inputs.hash();
var from = inputs.from().isDefined() ? inputs.from().get() : "";
var to = inputs.to().isDefined() ? inputs.to().get() : "";
var value = inputs.v();
var typ = inputs.typ();
//print(">>>",hash);
var output = hash + ": "+typ +": ("+ from + " -> " + to + " ("+value+"))";
var res = {tx_hash: hash, output: output};
Java.asJSONCompatible(res);