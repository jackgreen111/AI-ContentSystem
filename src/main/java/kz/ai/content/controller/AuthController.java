package kz.ai.content.controller;
import kz.ai.content.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
@Controller @RequestMapping("/auth")
public class AuthController {
    private final UserService userService;
    public AuthController(UserService us){ this.userService=us; }
    @GetMapping("/login") public String login(@RequestParam(required=false)String error,@RequestParam(required=false)String logout,Model m){
        if(error!=null)m.addAttribute("errorMessage","Неверный email или пароль.");
        if(logout!=null)m.addAttribute("logoutMessage","Вы вышли из системы.");
        return "auth/login";
    }
    @GetMapping("/register") public String register(){ return "auth/register"; }
    @PostMapping("/register") public String doRegister(@RequestParam String username,@RequestParam String email,@RequestParam String password,@RequestParam String confirmPassword,RedirectAttributes ra){
        if(!password.equals(confirmPassword)){ra.addFlashAttribute("errorMessage","Пароли не совпадают.");return "redirect:/auth/register";}
        if(!isValid(password)){ra.addFlashAttribute("errorMessage","Пароль: минимум 8 символов, заглавная + цифра.");return "redirect:/auth/register";}
        try{userService.register(username,email,password);ra.addFlashAttribute("successMessage","Регистрация успешна!");return "redirect:/auth/login";}
        catch(IllegalArgumentException e){ra.addFlashAttribute("errorMessage",e.getMessage());return "redirect:/auth/register";}
    }
    private boolean isValid(String p){return p.length()>=8&&p.chars().anyMatch(Character::isUpperCase)&&p.chars().anyMatch(Character::isDigit);}
}
