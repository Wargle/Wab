/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package control;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.Version;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import model.DAO;
import model.Element;
import model.MyList;
import model.User;
import model.AbstractComposite;
import org.h2.util.IOUtils;
import spark.Spark;
import static spark.Spark.before;
import static spark.Spark.get;
import static spark.Spark.post;
import static spark.Spark.staticFiles;
import static spark.route.HttpMethod.get;

/**
 * Classe Main qui recupère les requêtes HTTP, les redirige et renvoi les reponses HTTP
 * @author Alexis Arnould
 */
public class MainControl {

    private static Configuration cfg;
    
    /**
     * Permet de configurer Freemarker
     */
    private static void setConfiguration() {
        cfg = new Configuration();
        cfg.setClassForTemplateLoading(control.MainControl.class, "../view/templates");

        cfg.setIncompatibleImprovements(new Version(2, 3, 20));
        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.FRANCE);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
    }
    
    /**
     * Permet de convertir un fichier en String
     * @param file : le fichier
     * @return la version String du fichier
     */
    private static String convertFileToString(String file) {
        String content = "";
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                content +=str;
            }
            in.close();
        } 
        catch (Exception e) { 
            content = "Ya même un problème sur les fichiers :\'(";
        }
        return content;
    }
    
    /**
     * Permet de générer le vue HTML de toutes les listes d'un user
     * @param idUser : l'id du user en BDD
     * @return La vue HTML sous forme de string
     */
    private static String generateOutUserList(String idUser) {
        try {
            List<MyList> ls = DAO.getAllListByUser(idUser);

            Map<String, Object> input = new HashMap<>();

            Template template = cfg.getTemplate("userListsTemplate.ftl");
            input.put("lists", ls);
            
            StringWriter writer = new StringWriter();
            template.process(input, writer);
            
            return writer.toString();
        }
        catch (Exception e) {
            System.out.println(e.toString());
            return convertFileToString("src/view/out/400.html");
        }
    }
    
    /**
     * Permet de générer le vue HTML d'une liste selon l'id
     * @param idList : l'id de la list en BDD
     * @return La vue HTML sous forme de string
     */
    private static String generateOutList(String idUser, String idList) {
        try {
            MyList els = new MyList(DAO.getAllElementByList(idUser, idList));
            
            if(els.list.isEmpty())
                return generateOutUserList(idUser);
            
            Map<String, Object> input = new HashMap<>();
            input.put("title", "C'est genial mais pas trop");
            input.put("list", els.list);

            Template template = cfg.getTemplate("listTemplate.ftl");

            StringWriter writer = new StringWriter();
            template.process(input, writer);
            
            return writer.toString();
        }
        catch (Exception e) { 
            return convertFileToString("src/view/out/400.html");
        }
    }
    
    /**
     * @param args the command line arguments
     * @throws java.lang.Exception
     */
    public static void main(String[] args) throws Exception {    
        setConfiguration();
        staticFiles.externalLocation("src/view/out");
        
        before((req, res) -> {
            if(!DAO.testConnection()) {
                res.redirect("/500.html");
            }
        });
        
        get("/", (req, res) -> {
            String name = req.session().attribute("username");
            if (name == null) {
                return convertFileToString("src/view/out/connection.html");
            }
            else {
                res.redirect("/listes");
                return "";                
            }
        });
        post("/connection", (req, res) -> {
            String login = req.queryParams("login"), pw = req.queryParams("pw");
            if (DAO.canConnect(login, pw)) {
                req.session().attribute("username", login);
                res.redirect("/listes");
            }
            else 
                res.redirect("/401.html");
            return "";
        });
        get("/disconnect", (req, res) -> {
            req.session().removeAttribute("username");
            res.redirect("/");
            return "";
        });
        
        get("/listes", (req, res) -> {
            if(req.session().attribute("username") == null)
                return convertFileToString("src/view/out/401.html");
            return generateOutUserList("1");
        });

        get("/listes/createList", (req, res) -> {
            return convertFileToString("src/view/out/createList.html");
        });

        post("/listes/createList", (req, res) -> {
            return "ntm";
        });

        get("/listes/:idList", (req, res) -> {
            if(req.session().attribute("username") == null)
                return convertFileToString("src/view/out/401.html");
            return generateOutList("1", req.params("idList"));
        });

        get ("/createUser", (req, res) -> {
            return convertFileToString("src/view/out/createUser.html");
        });

        post ("/createUser", (req, res) -> {
            int id = 5;
            String n = req.queryParams("nom");
            String p = req.queryParams("prenom");
            String l = req.queryParams("login");
            String pa = req.queryParams("pw");
            User u = new User (id,n,p,l,pa);
            DAO.insertUser(u);
            res.redirect("/");
            return "";
        });
        
        Spark.notFound((req, res) -> {
            return convertFileToString("src/view/out/404.html");
        });
        Spark.internalServerError((req, res) -> {
            return convertFileToString("src/view/out/500.html");
        });
    }    
}