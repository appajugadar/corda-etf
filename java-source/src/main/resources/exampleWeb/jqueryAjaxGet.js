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
		  var restUrl = window.location + "../../../../api/rest/self-issue-cash?amount=" +amt+ "&currency="+currency;
    $.ajax({
    					  type : "GET",
                		  url : restUrl,
                          success: function(result){
                		        getCashBalance();
                			},
                			error : function(e) {
                				alert("ERROR IN ISSUE CASH :URL  "+restUrl, e);
                				console.log("ERROR IN ISSUE CASH :URL : "+restUrl, e);
                			}
                		});
});

    $("button[id='securityIssueButton']").click(function(data){
		  var quantity = $('input[name="quantity"]').val();
		  var securityName = $('input[name="securityName"]').val();
		  var restUrl = window.location + "../../../../api/rest/self-issue-security?quantity=" +quantity+ "&securityName="+securityName;
    $.ajax({
    					  type : "GET",
                		  url : restUrl,
                          success: function(result){
                		        	getSecurityBalance();
                			},
                			error : function(e) {
                				alert("ERROR IN ISSUE SECURITY :URL  "+restUrl, e);
                				console.log("ERROR IN ISSUE SECURITY :URL : "+restUrl, e);
                			}
                		});
});

    $("button[id='buyButton']").click(function(data){
		  var quantity = $('input[name="buyquantity"]').val();
		  var securityName = $('input[name="buysecurityName"]').val();
		  var restUrl = window.location + "../../../../api/rest/buy-security?quantity=" +quantity+ "&securityName="+securityName;
    $.ajax({
    					  type : "GET",
                		  url : restUrl,
                          success: function(result){
                		        		getBuyRequests();
                			},
                			error : function(e) {
                				alert("ERROR IN ISSUE SECURITY :URL  "+restUrl, e);
                				console.log("ERROR IN ISSUE SECURITY :URL : "+restUrl, e);
                			}
                		});
});


    $("button[id='sellButton']").click(function(data){
		  var quantity = $('input[name="sellquantity"]').val();
		  var securityName = $('input[name="sellsecurityName"]').val();
		  var restUrl = window.location + "../../../../api/rest/sell-security?quantity=" +quantity+ "&securityName="+securityName;
    $.ajax({
    					  type : "GET",
                		  url : restUrl,
                          success: function(result){
                		        	getSellRequests();
                			},
                			error : function(e) {
                				alert("ERROR IN ISSUE SECURITY :URL  "+restUrl, e);
                				console.log("ERROR IN ISSUE SECURITY :URL : "+restUrl, e);
                			}
                		});
});


	function getCashBalance(){
		$.ajax({
			type : "GET",
			url : window.location + "../../../../api/rest/cash-balance",
			success: function(result){
                $("#cashBalanceTable > tbody").html("");

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
                $("#securityBalanceTable > tbody").html("");
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