var user_params = {
   //from: "0x75e89d5979E4f6Fba9F97c104c2F0AFB3F1dcB88".toLowerCase(), 
   //from: "0x71a4cd515bca3227af77bb353075c58b145db415".toLowerCase(),
   from: "",
   
   //to: "",
   //to: "0x3fC91A3afd70395Cd496C647d5a6CC9D4B2b7FAD".toLowerCase(),
   //to: "0xdf3e18d64bc6a983f673ab319ccae4f1a57c7097".toLowerCase(),
   to: "".toLowerCase(),
   
   value: "10.0",
   //value: "",
};

var intercepted = [];

var hash = inputs.hash();
var from = inputs.from();
var to = inputs.to().isDefined() ? inputs.to().get() : "";
var value = inputs.v();
var status = inputs.st().isDefined() ? inputs.st().get() : 1;


intercepted[0] = (user_params.from == "") || (from == user_params.from) 
intercepted[1] = (user_params.to == "") || (to == user_params.to) 
intercepted[2] = (user_params.value == "") || (Number(value) >= Number(user_params.value) * 1e18 )

var final = true;
for(i=0; i<intercepted.length; i++) { final = final && intercepted[i]; }
// print(">>>",hash,":",intercepted," > ",final);

if( final ) {
    var output = "value = "+ value / 1e18 + ", to="+to+", from="+from;
    var res = {tx_hash: hash, output: output};
    Java.asJSONCompatible(res);
} else null;
