var hash = inputs.hash();
var from = inputs.from();
var to = inputs.to().isDefined() ? inputs.to().get() : "";
var value = inputs.v();
var status = inputs.st().isDefined() ? inputs.st().get() : 1;
//print(">>>",hash);
var output = hash + ": "+ status +": ("+ from + " -> " + to + " ("+value+"))";
var res = {tx_hash: hash, output: output};
Java.asJSONCompatible(res);