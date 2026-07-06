Funkcionalni URL-ovi (kroz API Gateway, http://localhost:8765)

Users service:

GET /users - lista korisnika (Basic Auth, OWNER ili ADMIN)

GET /users/email?email=... - korisnik po email-u (Basic Auth, OWNER ili ADMIN)

POST /users - kreiranje korisnika, body {email, password, role} (Basic Auth, OWNER ili ADMIN)

PUT /users - izmena korisnika, body {email, password, role} (Basic Auth, OWNER ili ADMIN)

DELETE /users?email=... - brisanje korisnika (Basic Auth, OWNER ili ADMIN)

Bank account service:

GET /accounts - lista naloga (Basic Auth, ADMIN)

GET /accounts/email?email=... - nalozi po email-u (Basic Auth, ADMIN ili USER - samo svoj)

POST /accounts - kreiranje naloga, body {email, currencyCode, amount} (Basic Auth, ADMIN)

PUT /accounts - izmena naloga, body {email, currencyCode, amount} (Basic Auth, ADMIN)

DELETE /accounts?email=...&currencyCode=... - brisanje naloga (Basic Auth, ADMIN)

Crypto wallet service:

GET /wallets - lista novcanika (Basic Auth, ADMIN)

GET /wallets/email?email=... - novcanici po email-u (Basic Auth, ADMIN ili USER - samo svoj)

POST /wallets - kreiranje novcanika, body {email, cryptoCurrencyCode, amount} (Basic Auth, ADMIN)

PUT /wallets - izmena novcanika, body {email, cryptoCurrencyCode, amount} (Basic Auth, ADMIN)

DELETE /wallets?email=...&cryptoCurrencyCode=... - brisanje novcanika (Basic Auth, ADMIN)

Currency exchange service:

GET /currency-exchange?from=...&to=... - kurs fiat valute (bez autentikacije)

Crypto exchange service:

GET /crypto-exchange?from=...&to=... - kurs crypto valute (bez autentikacije)

Currency conversion service:

GET /currency-conversion?from=...&to=...&quantity=... - razmena fiat valute (Basic Auth, samo USER)

Trade service:

GET /trade-service?from=...&to=...&quantity=... - razmena fiat/crypto valuta (Basic Auth, samo USER)


Kredencijali korisnika

owner@email.com / owner / OWNER

admin@email.com / admin / ADMIN

user@email.com / user / USER
