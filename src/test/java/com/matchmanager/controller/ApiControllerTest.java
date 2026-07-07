package com.matchmanager.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.matchmanager.dto.ShareViewDto;
import com.matchmanager.entity.User;
import com.matchmanager.model.Court;
import com.matchmanager.model.Game;
import com.matchmanager.model.Player;
import com.matchmanager.repository.UserRepository;
import com.matchmanager.security.CustomOAuth2UserService;
import com.matchmanager.security.SecurityConfig;
import com.matchmanager.security.UserPrincipal;
import com.matchmanager.service.DrawService;
import com.matchmanager.service.ExcelService;
import com.matchmanager.service.MatchGroupService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {
        DrawController.class,
        MatchGroupController.class,
        ShareController.class,
        AuthController.class
})
@AutoConfigureMockMvc
@Import(SecurityConfig.class)
class ApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private DrawService drawService;

    @MockBean
    private ExcelService excelService;

    @MockBean
    private MatchGroupService matchGroupService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private HttpSessionSecurityContextRepository securityContextRepository;

    @MockBean
    private CustomOAuth2UserService customOAuth2UserService;

    @Test
    void generateDrawReturnsCourts() throws Exception {
        List<Court> courts = sampleCourts();
        when(drawService.generateDraw(anyList(), eq(1), eq(2))).thenReturn(courts);

        mockMvc.perform(post("/api/draw")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "courtCount": 1,
                                  "gamesPerPlayer": 2,
                                  "players": [
                                    {"name":"김철수","grade":"A","rating":80,"gender":"남","age":30},
                                    {"name":"이영희","grade":"B","rating":70,"gender":"여","age":40},
                                    {"name":"박민준","grade":"C","rating":60,"gender":"남","age":45},
                                    {"name":"최지은","grade":"D","rating":50,"gender":"여","age":50}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].courtNumber").value(1))
                .andExpect(jsonPath("$[0].players").isArray())
                .andExpect(jsonPath("$[0].games[0].teamA1.name").value("김철수"));
    }

    @Test
    void generateDrawRejectsInvalidPlayers() throws Exception {
        mockMvc.perform(post("/api/draw")
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "players": [
                                    {"name":"","grade":"A","rating":80,"gender":"남","age":30}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(true))
                .andExpect(jsonPath("$.message").isString());
    }

    @Test
    void excelTemplateReturnsXlsxAttachment() throws Exception {
        when(excelService.createTemplate()).thenReturn(new byte[] {1, 2, 3});

        mockMvc.perform(get("/api/excel-template"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    @Test
    void saveDrawUsesAuthenticatedPrincipal() throws Exception {
        UserPrincipal principal = userPrincipal(7L);
        when(matchGroupService.saveDraw(eq(7L), any())).thenReturn(99L);

        mockMvc.perform(post("/api/draws")
                        .with(authentication(principal))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "화요 번개",
                                  "courtCount": 1,
                                  "gamesPerPlayer": 0,
                                  "content": [
                                    {
                                      "courtNumber": 1,
                                      "players": [
                                        {"name":"김철수","grade":"A","rating":80,"gender":"남","age":30},
                                        {"name":"이영희","grade":"B","rating":70,"gender":"여","age":40},
                                        {"name":"박민준","grade":"C","rating":60,"gender":"남","age":45},
                                        {"name":"최지은","grade":"D","rating":50,"gender":"여","age":50}
                                      ],
                                      "games": []
                                    }
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99));

        verify(matchGroupService).saveDraw(eq(7L), any());
    }

    @Test
    void shareCreateRejectsShortPassword() throws Exception {
        mockMvc.perform(post("/api/draws/1/share")
                        .with(authentication(userPrincipal(7L)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("공유 비밀번호는 4자 이상 30자 이하여야 합니다."));
    }

    @Test
    void updateScoresUsesAuthenticatedPrincipal() throws Exception {
        mockMvc.perform(put("/api/draws/1/scores")
                        .with(authentication(userPrincipal(7L)))
                        .with(SecurityMockMvcRequestPostProcessors.csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {"matchId": 10, "team1Score": 21, "team2Score": 18}
                                ]
                                """))
                .andExpect(status().isNoContent());

        verify(matchGroupService).updateScores(eq(1L), eq(7L), anyList());
    }

    @Test
    void shareViewReturnsLockedState() throws Exception {
        when(matchGroupService.getShareView(eq("token123"), any()))
                .thenReturn(new ShareViewDto("화요 번개", true, false, 8, 1, null));

        mockMvc.perform(get("/api/share/token123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("화요 번개"))
                .andExpect(jsonPath("$.requiresPassword").value(true))
                .andExpect(jsonPath("$.unlocked").value(false));
    }

    @Test
    void authMeReturnsUnauthorizedWhenAnonymous() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("로그인이 필요합니다."));
    }

    private List<Court> sampleCourts() {
        Player p1 = new Player("김철수", "A", 80, "남", 30);
        Player p2 = new Player("이영희", "B", 70, "여", 40);
        Player p3 = new Player("박민준", "C", 60, "남", 45);
        Player p4 = new Player("최지은", "D", 50, "여", 50);

        Game game = new Game(1, p1, p2, p3, p4, null, null);
        Court court = new Court(1, List.of(p1, p2, p3, p4));
        court.setGames(List.of(game));
        return List.of(court);
    }

    private UserPrincipal userPrincipal(Long id) {
        User user = new User("user" + id + "@example.com", "{noop}password", "테스터", null, User.PROVIDER_LOCAL);
        user.setId(id);
        return new UserPrincipal(user);
    }

    private RequestPostProcessor authentication(UserPrincipal principal) {
        return SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities()));
    }
}
