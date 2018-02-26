$(document).ready(function() {
	
	getCashBalance();
	getSecurityBalance();
	getBuyRequests();
    getSellRequests();
	getMyInfo();
	// DO GET

    $("button[id='cashIssueButton']").click(function(data){
		  var amt = $('input[name="amount"]').val();
		  var currency = $('input[name="currency"]').val();
          alert( "Data currency: " + currency +"  amt "+amt);


    $.ajax({
    					  type : "GET",
                		  url : window.location + "../../../../api/rest/self-issue-cash?amount="+AMT+"&currency"+currency,
                          success: function(result){
                		alert("SUCCESS: ", e);
                			},
                			error : function(e) {
                				alert("ERROR: ", e);
                				console.log("ERROR: ", e);
                			}
                		});
});





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

    			function getBuyRequests(){
        		$.ajax({
        			type : "GET",
        			url : window.location + "../../../../api/rest/BuyRequests",
        			success: function(result){
        				$.each(result, function(i, stateref){
                                        var buyRequestRow = '<tr>' +
                                                                '<td>' + stateref.state.data.linearId.id + '</td>' +
                                		                        '<td>' + stateref.state.data.securityName + '</td>' +
                                							    '<td>' + stateref.state.data.quantity + '</td>' +
                                							    '<td>' + stateref.state.data.buyer + '</td>' +
                                							    '<td>' + stateref.state.data.seller + '</td>' +
                                							    '<td>' + stateref.state.data.status + '</td>' +
                                						  '</tr>';
                                        $('#buyRequestTable tbody').append(buyRequestRow);

            		        });

            				$( "#buyRequestTable tbody tr:odd" ).addClass("info");
            				$( "#buyRequestTable tbody tr:even" ).addClass("success");
            			},
            			error : function(e) {
            				alert("ERROR: ", e);
            				console.log("ERROR: ", e);
            			}
            		});
            	}


function getSellRequests(){
        		$.ajax({
        			type : "GET",
        			url : window.location + "../../../../api/rest/SellRequests",
        			success: function(result){
        				$.each(result, function(i, stateref){
                                        var sellRequestRow = '<tr>' +
                                                                '<td>' + stateref.state.data.linearId.id + '</td>' +
                                		                        '<td>' + stateref.state.data.securityName + '</td>' +
                                							    '<td>' + stateref.state.data.quantity + '</td>' +
                                							    '<td>' + stateref.state.data.buyer + '</td>' +
                                							    '<td>' + stateref.state.data.seller + '</td>' +
                                							    '<td>' + stateref.state.data.status + '</td>' +
                                						  '</tr>';
                                        $('#sellRequestTable tbody').append(sellRequestRow);

            		        });

            				$( "#sellRequestTable tbody tr:odd" ).addClass("info");
            				$( "#sellRequestTable tbody tr:even" ).addClass("success");
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