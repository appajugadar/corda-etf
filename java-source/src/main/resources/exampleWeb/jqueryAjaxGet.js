$(document).ready(function() {
	
	getCashBalance();
	getSecurityBalance();
	getMyInfo();
	// DO GET
	function getCashBalance(){
		$.ajax({
			type : "GET",
			url : window.location + "../../../../api/rest/cash-balance",
			success: function(result){
				$.each(result, function(i, cash){
					var cashRow = '<tr>' +
										'<td>' + cash.currency + '</td>' +
										'<td>' + cash.quantity + '</td>' +
									  '</tr>';
					$('#cashBalanceTable tbody').append(cashRow);
					
		        });
				
				$( "#cashBalanceTable tbody tr:odd" ).addClass("info");
				$( "#cashBalanceTable tbody tr:even" ).addClass("success");
			},
			error : function(e) {
				alert("ERROR: ", e);
				console.log("ERROR: ", e);
			}
		});	
	}

		function getSecurityBalance(){
		$.ajax({
			type : "GET",
			url : window.location + "../../../../api/rest/security-balance",
			success: function(result){
				$.each(result, function(i, security){
                                var securityRow = '<tr>' +
                        		                        '<td>' + security.securityName + '</td>' +
                        							    '<td>' + security.quantity + '</td>' +
                        						  '</tr>';
                                $('#securityBalanceTable tbody').append(securityRow);

    		        });

    				$( "#securityBalanceTable tbody tr:odd" ).addClass("info");
    				$( "#securityBalanceTable tbody tr:even" ).addClass("success");
    			},
    			error : function(e) {
    				alert("ERROR: ", e);
    				console.log("ERROR: ", e);
    			}
    		});
    	}


function getMyInfo(){
    $.ajax({
        type : "GET",
        url : window.location + "../../../../api/rest/me",
    }).then(function(data) {
       $('.greeting-id').append(data.me);
       $('.greeting-id2').append(data.me);
    });
	}

})