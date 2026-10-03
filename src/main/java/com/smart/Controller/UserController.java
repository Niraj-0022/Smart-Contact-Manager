package com.smart.Controller;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.smart.dao.ContactRepository;
import com.smart.dao.MyOrderRepository;
import com.smart.dao.UserRepository;
import com.smart.entities.Contact;
import com.smart.entities.MyOrder;
import com.smart.entities.User;
import com.smart.helper.Message;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ContactRepository contactRepository;
    
    @Autowired
    private MyOrderRepository myOrderRepository;
	
	@ModelAttribute
    public void addCommonData(Model model, Principal principal) {
        String userName = principal.getName();
        System.out.println("USERNAME "+userName);
        
        User user = userRepository.getUserByUserName(userName);
        
        model.addAttribute("user", user);
    }


    @GetMapping("/index")
    public String dashboard(Model model,Principal principal) {
        
    	String userName=principal.getName();
    	System.out.println("USERNAME"+userName);
    	
    	User user=userRepository.getUserByUserName(userName);
    	
    	System.out.println("USER "+user);
    	model.addAttribute("user", user);
    	
    	return "user_dashboard";
    }
    
    @GetMapping("/add-contact")
    public String openContactForm(Model model) {
    	model.addAttribute("contact", new Contact());
    	
    	return "add_contact_form";
    }
    
 // ✅ SAVE CONTACT (POST ONLY)
    @PostMapping("/contact1")
    public String processContact(@ModelAttribute Contact contact,
                                 @RequestParam("profileImage") MultipartFile file,
                                 Principal principal,
                                 Model model) {

        try {
            User user = userRepository.getUserByUserName(principal.getName());

            if (!file.isEmpty()) {
                contact.setImage(file.getOriginalFilename());
                File saveFile = new ClassPathResource("static/img").getFile();
                Path path = Paths.get(saveFile.getAbsolutePath(), file.getOriginalFilename());
                Files.copy(file.getInputStream(), path, StandardCopyOption.REPLACE_EXISTING);
            }

            user.getContacts().add(contact);
            contact.setUser(user);
            this.userRepository.save(user);

            // Add success message
            model.addAttribute("message", new Message("Your contact is added!", "success"));

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("message", new Message("Something went wrong!", "danger"));
        }

        // Stay on the same page
        return "add_contact_form"; 
    }
    
    @GetMapping("/show-contacts/{page}")
    public String showContacts(
            @PathVariable("page") int page,
            Model m,
            Principal principal) {

        String userName = principal.getName();

        User user = this.userRepository.getUserByUserName(userName);

        List<Contact> contacts =
                this.contactRepository.findContactsByUser(user.getId());

        m.addAttribute("contacts", contacts);
        m.addAttribute("currentPage", page);
        m.addAttribute("totalPages", 1);

        return "show-contacts";
    }
    
    @GetMapping("delete/{cid}")
    public String deleteContact(@PathVariable("cid") Integer cid,Model model,HttpSession session) {
    	
    	Optional<Contact> contactOptional=this.contactRepository.findById(cid);
    	Contact contact=contactOptional.get();
    	
    	System.out.println("Contact "+contact.getcId());
    	
    	contact.setUser(null);
    	
    	this.contactRepository.delete(contact);
    	
    	session.setAttribute("message", new Message("Contact deleted successfully...","success"));
    	
    	return "redirect:/user/show-contacts/0";
    }
    
    @GetMapping("/update-contact/{cid}")
    public String updateForm(@PathVariable("cid") Integer cid, Model m) {
        m.addAttribute("title","Update Contact");
        Contact contact = this.contactRepository.findById(cid).get();
        m.addAttribute("contact",contact);
        return "update_form";
    }

    @PostMapping("/process-update")
    public String updateHandler(
            @ModelAttribute Contact contact,
            @RequestParam("profileImage") MultipartFile file,
            HttpSession session,
            Principal principal) {

        System.out.println("========== UPDATE ==========");
        System.out.println("Contact ID: " + contact.getcId());
        System.out.println("Name: " + contact.getName());
        System.out.println("Phone: " + contact.getPhone());
        System.out.println("Email: " + contact.getEmail());

        try {

            Optional<Contact> optionalContact =
                    contactRepository.findById(contact.getcId());

            if (!optionalContact.isPresent()) {

                session.setAttribute(
                        "message",
                        new Message("Contact not found!", "danger")
                );

                return "redirect:/user/show-contacts/0";
            }

            Contact oldContact = optionalContact.get();

            // Update normal fields
            oldContact.setName(contact.getName());
            oldContact.setSecondName(contact.getSecondName());
            oldContact.setPhone(contact.getPhone());
            oldContact.setEmail(contact.getEmail());
            oldContact.setWork(contact.getWork());
            oldContact.setDescription(contact.getDescription());

            // Update image only if a new image is selected
            if (!file.isEmpty()) {

                File imgDir =
                        new ClassPathResource("static/img").getFile();

                // Delete old image
                if (oldContact.getImage() != null
                        && !oldContact.getImage().isEmpty()) {

                    File oldFile =
                            new File(imgDir, oldContact.getImage());

                    if (oldFile.exists()) {
                        oldFile.delete();
                    }
                }

                // Save new image
                String fileName = file.getOriginalFilename();

                Path path = Paths.get(
                        imgDir.getAbsolutePath(),
                        fileName
                );

                Files.copy(
                        file.getInputStream(),
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );

                oldContact.setImage(fileName);
            }

            // Get logged-in user
            User user =
                    userRepository.getUserByUserName(principal.getName());

            oldContact.setUser(user);

            // Save the EXISTING contact
            contactRepository.save(oldContact);

            session.setAttribute(
                    "message",
                    new Message(
                            "Contact updated successfully!",
                            "success"
                    )
            );

            return "redirect:/user/" + oldContact.getcId() + "/contact";

        } catch (Exception e) {

            e.printStackTrace();

            session.setAttribute(
                    "message",
                    new Message(
                            "Something went wrong!",
                            "danger"
                    )
            );

            return "redirect:/user/update-contact/" + contact.getcId();
        }
    }
 
    @GetMapping("/profile")
    public String yourProfile(Model model)
    {
    	model.addAttribute("title","Profile Page");
    	return "profile";
    }
    
    @GetMapping("/{cId}/contact")
    public String showContactDetail(@PathVariable("cId") Integer cId, Model model)
    {
    	System.out.println("CID"+cId);
    	
    	Optional<Contact> contactOptional = this.contactRepository.findById(cId);
    	Contact contact = contactOptional.get();
    	
    	model.addAttribute("contact",contact);
    	return "contact_detail";
    	
    }

    @GetMapping("/settings")
    public String openSettings() {
    	return "settings";
    }
    
    @PostMapping("/change-password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,Principal principal,@RequestParam("newPassword") String newPassword,HttpSession session) {
    	System.out.println("OLD PASSWORD "+oldPassword);
    	System.out.println("NEW PASSWORD "+newPassword);
    	
    	String userName=principal.getName();
    	User currentUser=this.userRepository.getUserByUserName(userName);
    	System.out.println(currentUser.getPassword());
    	
    	if(this.bCryptPasswordEncoder.matches(oldPassword,currentUser.getPassword())) {
    		currentUser.setPassword(this.bCryptPasswordEncoder.encode(newPassword));
    		this.userRepository.save(currentUser);
    		session.setAttribute("message", new Message("Your Password is Successfully Changed!","success"));
    	}
    	else {
    		session.setAttribute("message", new Message("Please Enter Correct Old Password!","danger"));
    		return "redirect:/user/settings";
    	}
    	return "redirect:/user/index";
    }
    
    @PostMapping("/create_order")
    @ResponseBody
    public String createOrder(@RequestBody Map<String, Object> data,Principal principal) throws RazorpayException {

        System.out.println(data);

        int amt = Integer.parseInt(data.get("amount").toString());

        var client=new RazorpayClient("rzp_test_S7i6VnfDSHnVgo", "0b52kR9cvmMGOoQbqpOMcfIW");

        JSONObject ob = new JSONObject();
        ob.put("amount", amt * 100); // Razorpay expects amount in paise
        ob.put("currency", "INR");
        ob.put("receipt", "txn_235425");

        Order order = client.orders.create(ob);
        System.out.println(order);
        
        MyOrder myOrder=new MyOrder();
        
        myOrder.setAmount(order.get("amount")+"");
        myOrder.setOrderId(order.get("id"));
        myOrder.setPaymentId(null);
        myOrder.setStatus("created");
        myOrder.setUser(this.userRepository.getUserByUserName(principal.getName()));
        myOrder.setReceipt(order.get("receipt"));
        
        this.myOrderRepository.save(myOrder);
        
        // Build a response the frontend script.js can actually use
        JSONObject response = new JSONObject();
        response.put("id", order.get("id").toString());
        response.put("amount", order.get("amount").toString());
        response.put("status", "created");

        return response.toString();
    }

    @PostMapping("/update_order")
    @ResponseBody
    public String updateOrder(@RequestBody Map<String, Object> data) {

        System.out.println(data);

        // TODO: verify payment signature and persist payment status to DB here

        JSONObject response = new JSONObject();
        response.put("status", "success");

        return response.toString();
    }
    
}
