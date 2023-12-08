package world.ludium.education.announcement;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import world.ludium.education.article.Article;
import world.ludium.education.article.ArticleService;
import world.ludium.education.auth.LoginService;
import world.ludium.education.auth.ludium.LudiumUser;
import world.ludium.education.auth.ludium.LudiumUserService;
import world.ludium.education.course.*;
import world.ludium.education.course.Module;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/announcement", produces = "application/json")
public class AnnouncementController {
    private LoginService loginService;

    private ArticleService articleService;
    private LudiumUserService ludiumUserService;
    private ModuleService moduleService;

    public AnnouncementController(LoginService loginService, ArticleService articleService, LudiumUserService ludiumUserService, ModuleService moduleService) {
        this.loginService = loginService;
        this.articleService = articleService;
        this.ludiumUserService = ludiumUserService;
        this.moduleService = moduleService;
    }

    @GetMapping("")
    public ResponseEntity getAllAnnouncement() {
        return ResponseEntity.ok(articleService.getAllAnnouncement());
    }

    @PostMapping("")
    public ResponseEntity createCourse(@RequestParam String title,
                                       @RequestParam String content,
                                       @RequestParam String modules,
                                       @CookieValue(name = "access_token", required = false) String accessToken) {
        JsonNode googleUserApiData = null;

        try {
            googleUserApiData = loginService.getUserResource(accessToken, "google");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    new HashMap<String, String>() {
                        {
                            put("message", "인증에 실패했습니다.");
                            put("debug", e.getMessage());
                        }
                    });
        }

        LudiumUser ludiumUser = ludiumUserService.getUserByGglId(new BigInteger(googleUserApiData.get("id").toString().replaceAll("\"", "")));

        Article article = new Article();
        article.setTitle(title);
        article.setContent(content);
        article.setUsrId(ludiumUser.getId());

        try {
            List<Module> moduleList = Arrays.stream(modules.split("\\|"))
                    .map(moduleTitle -> {
                        Module module = new Module();
                        module.setTitle(moduleTitle);
                        return module;
                    })
                    .collect(Collectors.toList());

            articleService.createAnnouncementWithModule(article, moduleList);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    new HashMap<String, String>() {{
                        put("message", "수업을 만드는 중에 에러가 발생했습니다.");
                        put("debug", e.getMessage());
                    }}
            );
        }

        return ResponseEntity.ok(new HashMap<String, String>() {{
            put("title", title);
            put("content", content);
        }});
    }

    @GetMapping("/{announcementId}")
    public ResponseEntity getAnnouncement(@PathVariable UUID announcementId) {
        CourseDTO courseDTO = new CourseDTO();
        try {
            Article announce = articleService.getArticle(announcementId);
            List<Module> modules = moduleService.getAllModulesByCourse(announcementId);

            courseDTO.setId(announce.getId());
            courseDTO.setTitle(announce.getTitle());
            courseDTO.setContent(announce.getContent());
            courseDTO.setModules(modules);
        } catch (Exception e) {
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new HashMap<>() {{
                put("message", "수업을 불러오는 중에 에러가 발생했습니다.");
                put("debug", e.getMessage());
            }});
        }
        return ResponseEntity.ok(courseDTO);
    }

    @GetMapping("/{announcementId}/{moduleId}")
    public ResponseEntity getModule(@PathVariable UUID moduleId) {
        ModuleDTO moduleDTO = new ModuleDTO();

        try {
            Module module = moduleService.getModule(moduleId);
            List<ModuleReference> moduleReferences = moduleService.getAllModuleReferenceByModuleId(moduleId);

            moduleDTO.setId(moduleId);
            moduleDTO.setTitle(module.getTitle());
            moduleDTO.setContent(module.getContent());
            moduleDTO.setCategory(module.getCategory());
            moduleDTO.setModuleReferences(moduleReferences);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new HashMap<>() {{
                        put("message", "모듈을 불러오는 중에 에러가 발생했습니다.");
                        put("debug", e.getMessage());
                    }});
        }

        return ResponseEntity.ok(moduleDTO);
    }

    @PutMapping("/{announcementId}/module/{moduleId}")
    public ResponseEntity updateModule(@PathVariable UUID announcementId,
                                       @PathVariable UUID moduleId,
                                       @RequestParam String title,
                                       @RequestParam String content) {
        Module module = new Module();
        module.setId(moduleId);
        module.setTitle(title);
        module.setContent(content);
        module.setCategory("");
        module.setCrsId(announcementId);

        return ResponseEntity.ok(moduleService.updateModule(module));
    }
}
