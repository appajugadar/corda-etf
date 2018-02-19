Rest API URLS

Buyer

http://localhost:10007/api/rest/self-issue-cash?amount=100&currency=GBP

http://localhost:10007/api/rest/buy-security?quantity=10&securityName=OLI


http://localhost:10007/api/rest/BuyRequests

http://localhost:10007/api/rest/cash-balance

http://localhost:10007/api/rest/security-balance



Seller

http://localhost:10010/api/rest/self-issue-security?quantity=100&securityName=OLI

http://localhost:10010/api/rest/sell-security?quantity=10&securityName=OLI

http://localhost:10010/api/rest/SellRequests

http://localhost:10010/api/rest/security-balance

http://localhost:10010/api/rest/cash-balance





Depository

http://localhost:10013/api/rest/SellRequests

http://localhost:10013/api/rest/BuyRequests


Regulator

http://localhost:10016/api/rest/SellRequests

http://localhost:10016/api/rest/BuyRequests


JVM Param for testcase

-ea -javaagent:../lib/quasar.jar
