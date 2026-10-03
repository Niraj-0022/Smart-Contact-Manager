package com.smart.Controller;

import java.util.Random;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.smart.dao.UserRepository;
import com.smart.entities.User;
import com.smart.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ForgetController {
	Random random=new Random(1000);
	
	@Autowired
    private EmailService emailService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder bcrypt;


	@RequestMapping("/forget")
	public String openEmailForm() {
		return "forget_email_form";
	}
	
	@PostMapping("/send-otp")
	public String sendOTP(@RequestParam("email") String email,
	                      HttpSession session) {

	    int otp = 100000 + random.nextInt(900000);

	    System.out.println("OTP = " + otp);

	    String subject = "OTP From SCM";
	    String message = "<h1>OTP = " + otp + "</h1>";
	    String to = email;

	    boolean flag = this.emailService.sendEmail(subject, message, to);

	    if (flag) {

	        session.setAttribute("myotp", otp);
	        session.setAttribute("email", email);

	        return "verify_otp";

	    } else {

	        session.setAttribute("message", "Check your email id !!");

	        return "forget_email_form";
	    }
	}	
	@PostMapping("/verify-otp")
	public String verifyOtp(@RequestParam("otp") int otp,
	                        HttpSession session) {

	    Integer myOtp = (Integer) session.getAttribute("myotp");
	    String email = (String) session.getAttribute("email");

	    System.out.println("User OTP: " + otp);
	    System.out.println("Our OTP: " + myOtp);
	    System.out.println("Email: " + email);

	    // Check session
	    if (myOtp == null || email == null) {
	        session.setAttribute("message",
	                "Session expired. Please request a new OTP.");
	        return "forget_email_form";
	    }

	    // Verify OTP
	    if (myOtp.intValue() == otp) {

	        User user = this.userRepository.getUserByUserName(email);

	        if (user == null) {
	            session.setAttribute("message", "User not found.");
	            return "forget_email_form";
	        }

	        // OTP verified
	        session.setAttribute("otpVerified", true);

	        return "password_change_form";

	    } else {

	        session.setAttribute("message", "You have entered wrong OTP.");

	        return "verify_otp";
	    }
	}
	
	@PostMapping("/change-password")
	public String changePassword(@RequestParam("newpassword") String newpassword,HttpSession session) {
		String email=(String)session.getAttribute("email");
		User user=this.userRepository.getUserByUserName(email);
		user.setPassword(this.bcrypt.encode(newpassword));
		this.userRepository.save(user);
		return "redirect:/signin?change=password changed successfully...";
	}
}
