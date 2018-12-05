import org.testng.annotations.Test;
import io.restassured.RestAssured;
import io.restassured.authentication.PreemptiveBasicAuthScheme;
import io.restassured.http.Method;
import io.restassured.path.json.JsonPath;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;

import org.json.simple.JSONObject;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.support.ui.Select;
import org.testng.Assert;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeSuite;

//Selenium Webdriver Java
//TestNG
//Rest-Assured
//Simple JSON Library

public class SaleChargeRefund {
	
	private static WebDriver driver = null;
	String saleToken, saleObject;
	
	@BeforeSuite
	public void beforeSuit() 
	{		
		System.setProperty("webdriver.gecko.driver", "geckodriver.exe"); //Geckodriver for Latest Firefox browser
		
		//Authentication for Paytrek API
		PreemptiveBasicAuthScheme authScheme = new PreemptiveBasicAuthScheme();
		authScheme.setUserName("test_tr");
		authScheme.setPassword("Tr112233");
		RestAssured.authentication = authScheme;
	}
	
	//Simply Access to API and Get an OK Respond to Check Connection Health
	@Test (priority = 0)
	public void connectionHealthCheck() {
		
		RequestSpecification httpRequest = RestAssured.given();		
		Response response = httpRequest.request(Method.GET, "https://sandbox.paytrek.com/api/v1/");
		
		int statusCode = response.getStatusCode();		
		Assert.assertEquals(statusCode, 200, "OK"); //Status Code 200 (OK) is Expected to Pass the Test
		
		System.out.println("Test 1 | Connection Check: Success (" + response.getStatusLine() +")");
	}
	
	//Simple JSON Library USED for POST Data
	@SuppressWarnings("unchecked")
	@Test (priority = 1)
	public void createASale() {		
		
		RequestSpecification httpRequest = RestAssured.given();
		
		long unitTime = System.currentTimeMillis() / 1000L; //Unix Timestamp to use as OrderID
		String orderID = Long.toString(unitTime);
		
		JSONObject requestParams = new JSONObject();
		JSONObject saleData = new JSONObject();
		requestParams.put("currency", "/api/v1/currency/TRY/");
		requestParams.put("order_id", orderID);
		requestParams.put("amount", 3);
		requestParams.put("secure_option", "No");
		requestParams.put("half_secure", "Yes");
		requestParams.put("return_url", "#");
		requestParams.put("cancel_url", "#");
		requestParams.put("customer_first_name", "John");
		requestParams.put("customer_last_name", "Doe");
		requestParams.put("customer_email", "johndoe@gmail.com");
		requestParams.put("customer_ip_address", "212.57.9.204");
		requestParams.put("billing_address", "123 Market St. San Francisco");
		requestParams.put("billing_city", "San Francisco");
		requestParams.put("billing_state", "CA");
		requestParams.put("billing_country", "US");		
		requestParams.put("billing_zipcode", "34410");
		requestParams.put("billing_phone", "+901112233");
		requestParams.put("sale_data", saleData);	
		
		//requestParams.put("fraud_check_enabled", "false");
		//requestParams.put("installment", "1");		
		//requestParams.put("pre_auth", "false");		
		//requestParams.put("items", "");		
		//HashMap Used for Multi-Dimentional Array
		/*Map<String,String> items = new HashMap<>();
		items.put("name","Ramada Hotel");
		items.put("photo","http://d1ldkdiqjyt22f.cloudfront.net/hotelimages/UKSFLE/1937525_140x110.jpg");
		items.put("quantity","1");
		items.put("unit_price","666.6");
		System.out.println("Items" + items);*/		
		
		//Header and Body Set as JSON
		httpRequest.header("Content-Type", "application/json");		
		httpRequest.body(requestParams.toJSONString());
		
		Response response = httpRequest.request(Method.POST, "https://sandbox.paytrek.com/api/v1/sale/");
		
		//201 (CREATED) Code Expected to PASS
		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 201);
		
		//Simple JSON Library's Path Evaluator Used to Catch Response Node Data
		JsonPath jsonPathEvaluator = response.jsonPath();
		
		//Sale Token and Object Saved
		saleToken = jsonPathEvaluator.get("token");
		saleObject = jsonPathEvaluator.get("resource_uri");
		
		System.out.println("Test 2 | Create Sale: Success (" + response.getStatusLine() + ")");	
		System.out.println("Test 2 | Sale Token: " + saleToken);
		System.out.println("Test 2 | Sale Object: " + saleObject);
		System.out.println("Test 2 | Order ID: " + orderID);
		//System.out.println("Test 2 | Body: " + response.asString());
	}
	
	//1ST METHOD TO CHARGE CUSTOMERS
	//Charge Process on the Front-End (Common Payment Page)
	@Test (priority = 2, enabled = false)
	public void chargeUI() throws InterruptedException {
		//Execute Browser, Maxime It and Navigate to Payment Page
		driver = new FirefoxDriver();
		driver.manage().window().maximize();
		driver.get("https://sandbox.paytrek.com/?token=" + saleToken);
		
		//Fill the Required Payment Data		
		driver.findElement(By.id("id_number")).sendKeys("4508034508034509");		
		Thread.sleep(1000);	//Unorthodox way to use wait, a workaround would be proper	
		driver.findElement(By.id("id_card_holder_name")).click(); //Needed to trigger AJAX call
		Thread.sleep(1000);
		driver.findElement(By.id("id_card_holder_name")).sendKeys("John Doe");
		Thread.sleep(1000);
		
		//WebDriverWait wait = new WebDriverWait(driver, 10);
		//assertTrue(wait.until(ExpectedConditions.invisibilityOfElementLocated(By.xpath("/html/body/script[6]"))));		
		
		Select ed_month = new Select(driver.findElement(By.cssSelector("#id_expiration_0")));
		ed_month.selectByVisibleText("12");
		Thread.sleep(1000);
		Select ed_year = new Select(driver.findElement(By.cssSelector("#id_expiration_1")));
		ed_year.selectByVisibleText("2018");
		Thread.sleep(1000);
		
		driver.findElement(By.id("id_cvc")).sendKeys("000");
		Thread.sleep(1000);
		driver.findElement(By.className("button-pay")).click();
		//Thread.sleep(3000);
		
		String preProcess = driver.findElement(By.cssSelector("body > div > div.popup-box > h1")).getText();
		Assert.assertEquals(preProcess, "Thanks!");
		System.out.println("Test 3 | Charging Customer (UI): Success");
	}	
	
	//2ND METHOD TO CHARGE CUSTOMER
	@SuppressWarnings("unchecked")
	@Test (priority = 3, enabled = true)
	public void chargeEndpoint() {		
		
		RequestSpecification httpRequest = RestAssured.given();
		
		JSONObject requestParams = new JSONObject();
		requestParams.put("number", "4508034508034509");
		requestParams.put("expiration", "12/2018");
		requestParams.put("cvc", "000");
		requestParams.put("card_holder_name", "John Doe");
		requestParams.put("sale", saleObject);		
		
		//Header and Body Set as JSON
		httpRequest.header("Content-Type", "application/json");		
		httpRequest.body(requestParams.toJSONString());
		
		Response response = httpRequest.request(Method.POST, "https://sandbox.paytrek.com/api/v1/charge/");
		
		//201 (CREATED) Code Expected to PASS
		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 201);
		
		System.out.println("Test 3 | Charging Customer (API): Success (" + response.getStatusLine() + ")");			
	}
	
	//Verify the Success of Recent Charge
	@Test (priority = 4)
	public void chargeResultCheck() {
		//Get the Response of Corresponding Token and Catch the "status" Node		
		RequestSpecification httpRequest = RestAssured.given();		
		Response response = httpRequest.request(Method.GET, "https://sandbox.paytrek.com" + saleObject);
		
		JsonPath jsonPathEvaluator = response.jsonPath();		
		String status = jsonPathEvaluator.get("status");
		
		//Verify that the status is "Paid" to pass the test
		Assert.assertEquals(status, "Paid");

		System.out.println("Test 4 | Final Charge Status: " + status + " (" + response.getStatusLine() + ")");				
	}
	
	//Refund Process
	@SuppressWarnings("unchecked")
	@Test (priority = 4)
	public void refund() {
		//Send the optional Amount Data with Token of Recent Sale		
		RequestSpecification httpRequest = RestAssured.given();
		
		JSONObject requestParams = new JSONObject();
		requestParams.put("amount", 3);
		
		httpRequest.header("Content-Type", "application/json");		
		httpRequest.body(requestParams.toJSONString());
				
		Response response = httpRequest.post("https://sandbox.paytrek.com/api/v1/refund/" + saleToken + "/");
		
		//200 (OK) Status Code is Expected to Pass the Test
		int statusCode = response.getStatusCode();
		Assert.assertEquals(statusCode, 200);		
		
		JsonPath jsonPathEvaluator = response.jsonPath();		
		String succeed = jsonPathEvaluator.get("succeeded").toString();
		System.out.println("Test 5 | Refund Succeeded: " + succeed + " (" + response.getStatusLine() + ")");
	}
	
	//Complete This Suit and Close Driver
	@AfterSuite
	public void afterSuit() 
	{	
		driver.quit();
	}
}