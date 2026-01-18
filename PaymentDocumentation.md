* PAYMENT DOCUMENTATION *

1. Starting by creating a Razorpay Account, KYC and start development in Test Mode.

2. From settings generate the API key for the key_id and key_secret

3. Add the below lines of code in application.properties

/* CODE

	razorpay.key.id=${RAZORPAY_KEY_ID}
	razorpay.key.secret=${RAZORPAY_KEY_SECRET}
	
*/

4. Set the RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET in powershell by the following command lines

/* COMMAND LINES

setx RAZORPAY_KEY_ID "{Razorpay Key ID}"
setx RAZORPAY_KEY_SECRET "{Razorpay Key Secret}"
	
*/

5. Since we don't want to expose our RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET 
we do not directly add it to application.properties file directly.

6. In the Payment Entity we use paise instead of rupees to avoid floating points
Ex - ₹ 245.75 -> 24575 paise
We avoided the floating point successfully 

7. To create Razorpay Order in PaymentService.java

	NOTE - This method can give Exceptions (discussed on Line 90) and hence we throw RazorpayException

	a. First create a method returning Map<String,Object> since we want a HashMap() having amount and currency as parameters


	b. Update the amount and currency in the payment object and initialize the RazorpayClient with
	KEY_ID and KEY_SECRET

	/*
		Payment payment = new Payment();
        payment.setAmount(amount);
        payment.setCurrency(currency);
        payment = paymentRepository.save(payment);

     	//Razorpay client
        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);
	*/

	c. Later initialize JSONObject to create order request
		/*
			JSONObject orderRequest = new JSONObject();
			orderRequest.put("amount", amount); // paise
			orderRequest.put("currency", currency);
			orderRequest.put("receipt", "rcpt_" + payment.getId());
		*/

		from the JSON's object the following Response is expected by Razorpay's backend
		{
			"amount": 50000, ---> For Razorpay
			"currency": "INR", ---> For Razorpay
  			"receipt": "rcpt_1" --> For Internal Reference
		}

		Actual API call
		SDK converts JSONObject → HTTP POST request

		Calls Razorpay endpoint:

			POST /v1/orders


		Razorpay:

			Validates API keys
			Validates amount & currency
			Creates an Order on their servers

		Razorpay responds with JSON:

			{
			"id": "order_Nx8abc123",
			"amount": 50000,
			"currency": "INR",
			"status": "created",
			...
			}

		SDK wraps this response into an Order object

		So order now represents Razorpay’s order, not your DB entity.
		
			/*
				Order order = razorpayClient.orders.create(orderRequest); 
			*/

			order objects stores the following

			/*
				order.get("id");        // Razorpay order ID
				order.get("amount");    // amount
				order.get("currency");  // currency
				order.get("status");    // created
			*/
	
		You store:
			/*
				payment.setRazorpayOrderId(order.get("id"));
			*/
		
		This is the bridge between:

			Your DB
			Razorpay’s system
			
	d. Save Razorpay Order Id

		/*
			payment.setRazorpayOrderId(order.get("id"));
			paymentRepository.save(payment);
		*/
	
	e. To give response to the frontend
		
		/*
			Map<String, Object> response = new HashMap<>();
			response.put("paymentId", payment.getId());
			response.put("razorpayOrderId", order.get("id"));
			response.put("amount", amount);
			response.put("currency", currency);
			response.put("key", keyId);
		*/

8. API designed in PaymentController.java

	a. Annotation @PostMapping

	b. Return type ResponseEntity<Map<String, Object>>
		Here we throw an Exception 
			
			/*
				throws RazorpayException
			*/
		
		It is thrown to handle the following cases:
			~API key is invalid
			~Network error
			~Razorpay server error
			~Bad request (amount, currency, etc.)

9. Payment Verification

	Considering the verify PaymentService.java verifyPaymentSignature method
	
	a. Build the Payload
	
	/*
		String payload = razorpayOrderId + "|" + razorpayPaymentId;
	*/
		This is mandated by Razorpay.
		
		They define the exact string to sign as:
		
		order_id|payment_id

	/*
		Mac mac = Mac.getInstance("HmacSHA256");
	*/
	
	This tells Java:

		“I want to use HMAC with SHA-256.”
		
		Important concepts:
		
			~HMAC = Hash-based Message Authentication Code		
			~SHA-256 = cryptographic hash function		
			~Used for integrity + authenticity	
				