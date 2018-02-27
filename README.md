
GUI URL  

AP1  :   http://localhost:10007/web/example/index.html

AP2   :  http://localhost:10010/web/example/index.html

Depository  : http://localhost:10013/web/example/index.html

Regulator  :  http://localhost:10016/web/example/index.html

Rest API URLS

Buyer

http://localhost:10007/api/rest/self-issue-cash?amount=100&currency=GBP

http://localhost:10007/api/rest/buy-security?quantity=100&securityName=GLD


http://localhost:10007/api/rest/BuyRequests

http://localhost:10007/api/rest/cash-balance

http://localhost:10007/api/rest/security-balance



Seller

http://localhost:10010/api/rest/self-issue-security?quantity=100&securityName=GLD

http://localhost:10010/api/rest/sell-security?quantity=100&securityName=GLD

http://localhost:10010/api/rest/SellRequests

http://localhost:10010/api/rest/security-balance

http://localhost:10010/api/rest/cash-balance





Depository

http://localhost:10013/api/rest/SellRequests

http://localhost:10013/api/rest/BuyRequests


Regulator

http://localhost:10016/api/rest/SellRequests

http://localhost:10016/api/rest/BuyRequests


--CP Flow

http://localhost:10007/api/rest/self-issue-cp?quantity=100&currency=GBP

http://localhost:10007/api/rest/cp-balance

http://localhost:10007/api/rest/move-cp?receiverPartyName=AP2

http://localhost:10010/api/rest/cp-balance


JVM Param for testcase

-ea -javaagent:../lib/quasar.jar
