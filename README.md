# Bank Account System
A Spring Boot application that tracks a single bank account’s balance through credit and debit transactions, and periodically sends the last _N_ transactions (default 1,000) to an audit system in batches that minimize the number of submissions while ensuring no batch exceeds £1,000,000 in total transaction value.

# Getting Started
## Clone repository
> git clone https://github.com/Ashika-Shrivastaw/bank-account-system.git

## Build & run tests
> mvn clean install

## Launch the application
> mvn spring-boot:run

## REST API Usage
### Submit a Transaction
> curl -X POST http://localhost:8080/api/account/transaction \
>     -H "Content-Type: application/json" \
>     -d '{ "amount": 250.0 }'

Positive = credit, negative = debit.

Returns 400 if |amount| < 200 or |amount| > 500000.

### Check Balance
> curl http://localhost:8080/api/account/balance

## Audit Submission
After every 1,000 transactions (configurable), the app prints the audit batches to the console. Example:
> **Example Audit Output**
> ```text
> Processed 1,000 transactions into 3 batches in 12ms
> {
>   submission: {
>     batches: [
>       {
>         totalValueOfAllTransactions: 1000000.00
>         countOfTransactions: 172
>       },
>       {
>         totalValueOfAllTransactions: 464500.00
>         countOfTransactions: 656
>       },
>       {
>         totalValueOfAllTransactions: 1000000.00
>         countOfTransactions: 172
>       }
>     ]
>   }
> }
> ```

### Manual Testing with Postman
1. Check > [bank-account-system-postman_collection script](https://github.com/Ashika-Shrivastaw/bank-account-system/blob/main/bank-account-system-postman_collection%20script.txt)
2. In the Pre-request Script, a small loop will fire 1,000 mixed credit/debit requests at full speed.
3. Run the collection; watch your IDE console for the result after audit submission.


