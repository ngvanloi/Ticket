package io.group7.GBUH.booking.controller;

import java.io.IOException;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.naming.factory.SendMailFactory;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.circuitbreaker.EnableCircuitBreaker;
import org.springframework.core.env.Environment;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixProperty;


import io.group7.GBUH.booking.entity.Booking;

@EnableCircuitBreaker
@RestController
@RequestMapping("/")
public class HomeController {
	@Autowired
	private RestTemplate restTemplate;
	
	@Autowired
	private JavaMailSender javaMailSender;
	
	@Autowired
	private Environment env;
	

	public void sendMail(String error) {
		SimpleMailMessage smg = new SimpleMailMessage();
		smg.setTo("nguyenloi7412@gmail.com");
		smg.setSubject("Error Server");
		smg.setText(error);
		
		System.out.println("Send mail");
		javaMailSender.send(smg);
		System.out.println("Send mail successfull");
	}
	
	@RequestMapping("/")
	public String home() {
		// This is useful for debugging
		// When having multiple instance of gallery service running at different ports.
		// We load balance among them, and display which instance received the request.
		return "Hello from Booking Service running at port: " + env.getProperty("local.server.port");
	}
  
	@HystrixCommand(fallbackMethod="errorGetTickets")
	@RequestMapping("/ticket/{firm}")
	public Booking getBooking(@PathVariable String firm) {
		
		Booking booking = new Booking();
		org.json.simple.JSONArray array = new org.json.simple.JSONArray();
		String[] hang = firm.split(",");
		for (int i = 0; i < hang.length; i++) {
			String result = restTemplate.getForObject("http://"+hang[i]+"-gateway/"+hang[i]+"/tickets", String.class);	
			try {
				@SuppressWarnings("unchecked")
				Object obj = new JSONParser().parse(result);
				sendMail(hang[i] + "successfull");
				JSONObject ob = (JSONObject) obj;
				JSONArray arr = (JSONArray) ob.get("ticket");
				JSONObject newObj = new JSONObject();
				newObj.put(hang[i], arr);
				array.add(newObj);
			}catch (Exception e) {
				continue;
			}
		}
		booking.setTicket((List<Object>) array);
		return booking;
	}
	
	public Booking errorGetTickets(String firm) throws MessagingException, IOException{
		Booking tc = new Booking();
		String[] hang = firm.split(",");
		JSONArray array = new JSONArray();
		for (int i = 0; i < hang.length; i++) {
			JSONParser jsParser = new JSONParser();
			Object obj;
			JSONObject jsObj = new JSONObject();
			try {
				String result = restTemplate.getForObject("http://"+hang[i]+"-gateway/"+hang[i]+"/tickets", String.class);	
				obj = jsParser.parse(result);
			} catch (Exception e) {
				obj = null;
			}
			if(obj == null) {
				jsObj.put(hang[i],hang[i] + " error");
				sendMail(hang[i] + " error");
				array.add(jsObj);
			}else {
				JSONObject ob = (JSONObject) obj;
				JSONArray arr = (JSONArray) ob.get("ticket");
				JSONObject newObj = new JSONObject();
				newObj.put(hang[i], arr);
				array.add(newObj);
			}
		}
		tc.setTicket((List<Object>) array);
		return tc;
				
	}
	
	@RequestMapping("/{fim}/{ngaydi}/{ngayden}/{noidi}/{noiden}/{nguoilon}/{treem}/{embe}")
	public Booking getBooking(@PathVariable String fim,@PathVariable String ngaydi,@PathVariable String ngayden,@PathVariable String noidi,@PathVariable String noiden,@PathVariable String nguoilon,@PathVariable String treem,@PathVariable String embe) {
		Booking booking = new Booking();
		org.json.simple.JSONArray array = new org.json.simple.JSONArray();
		String[] hang = fim.split(",");
		for (int i = 0; i < hang.length; i++) {
			String result = restTemplate.getForObject("http://"+hang[0]+"-gateway/"+hang[0]+"/"+ngaydi+"/"+ngayden+"/"+noidi+"/"+noiden+"/"+nguoilon+"/"+treem+"/"+embe, String.class);	
			try {
				Object obj = new org.json.simple.parser.JSONParser().parse(result);
				org.json.simple.JSONObject ob = (org.json.simple.JSONObject) obj;
				org.json.simple.JSONArray arr = (org.json.simple.JSONArray) ob.get("ticket");
				org.json.simple.JSONObject newObj = new org.json.simple.JSONObject();
				newObj.put(hang[i], arr);
				array.add(newObj);
			}catch (org.json.simple.parser.ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		booking.setTicket((List<Object>) array);
		return booking;
		
	}
	// -------- Admin Area --------
	// This method should only be accessed by users with role of 'admin'
	// We'll add the logic of role based auth later
	@RequestMapping("/admin")
	public String homeAdmin() {
		return "This is the admin area of Gallery service running at port: " + env.getProperty("local.server.port");
	}
}