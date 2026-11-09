package com.lab.paperarchive.paper;

import com.lab.paperarchive.paper.dto.PaperListItem;
import com.lab.paperarchive.user.LabUserDetails;
import com.lab.paperarchive.user.Role;
import com.lab.paperarchive.user.Status;
import com.lab.paperarchive.user.User;
import com.lab.paperarchive.user.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.UUID;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"logging.level.org.hibernate.orm.jdbc.bind=WARN",
        "spring.jpa.properties.hibernate.show_sql=false"})
@AutoConfigureMockMvc
@Transactional
class PaperListSortingTest {
    @Autowired MockMvc mvc;
    @Autowired PaperRepository papers;
    @Autowired FolderRepository folders;
    @Autowired TagRepository tags;
    @Autowired UserRepository users;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;

    private final String keyword = "sort-" + UUID.randomUUID();
    private LabUserDetails principal;
    private Long folderId;

    @BeforeEach
    void preparePapersWithDifferentTitleAndDateOrders() {
        var account = users.saveAndFlush(User.builder().email(keyword + "@local.test")
                .name("정렬 검증").password("unused").role(Role.MEMBER).status(Status.ACTIVE).build());
        principal = new LabUserDetails(account);
        var folder = folders.saveAndFlush(Folder.of(keyword));
        folderId = folder.getId();
        var tag = tags.saveAndFlush(Tag.of(keyword));
        String[] titles = {"D", "A", "F", "B", "G", "C", "E"};
        int[] days = {4, 2, 7, 3, 1, 6, 5};
        for (int i = 0; i < titles.length; i++) {
            var paper = Paper.builder().title(keyword + " " + titles[i]).uploader(account).build();
            paper.assignFolder(folder);
            paper.addTag(tag);
            papers.saveAndFlush(paper);
            jdbc.update("update papers set created_at = ? where id = ?",
                    LocalDateTime.of(2026, 1, days[i], 12, 0), paper.getId());
        }
        // A matching paper outside this folder must never leak into the sorted pages.
        var outside = Paper.builder().title(keyword + " 0").uploader(account).build();
        outside.addTag(tag);
        papers.saveAndFlush(outside);
        em.clear();
    }

    @ParameterizedTest
    @CsvSource({", FCEDBAG", "default, FCEDBAG", "date-desc, FCEDBAG",
            "date-asc, GABDECF", "title, ABCDEFG", "unknown, FCEDBAG"})
    void sortsTheWholeFolderBeforePagination(String order, String expected) throws Exception {
        var actual = new ArrayList<String>();
        for (int page = 0; page < 4; page++) {
            var result = request(order, page);
            Page<PaperListItem> listing = listing(result);
            assertThat(listing.getTotalElements()).isEqualTo(7);
            actual.addAll(listing.getContent().stream().map(p -> p.title().substring(keyword.length() + 1)).toList());
        }
        assertThat(actual).containsExactlyElementsOf(expected.chars().mapToObj(c -> String.valueOf((char) c)).toList());
    }

    @Test
    void keepsSortAndFiltersInPageLinksAndSearchForm() throws Exception {
        var result = request("title", 1);
        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(html).contains("value=\"title\" selected=\"selected\"", "form=\"paperSearch\"",
                "name=\"size\" value=\"2\"", "name=\"tag\" value=\"" + keyword + "\"");
        var nav = Pattern.compile("<nav class=\"pagination page-blocks\"[\\s\\S]*?</nav>").matcher(html);
        assertThat(nav.find()).isTrue();
        var links = Pattern.compile("href=\"([^\"]+)\"").matcher(nav.group());
        var hrefs = new ArrayList<String>();
        while (links.find()) hrefs.add(links.group(1));
        assertThat(hrefs).hasSize(3).allSatisfy(href -> assertThat(href)
                .contains("order=title", "size=2", "q=" + keyword, "tag=" + keyword, "folder=" + folderId));
    }

    @Test
    void usesIdToKeepEqualDatesStableAcrossPages() throws Exception {
        jdbc.update("update papers set created_at = ? where folder_id = ?",
                LocalDateTime.of(2026, 1, 1, 12, 0), folderId);
        em.clear();
        var ids = new ArrayList<Long>();
        for (int page = 0; page < 4; page++) {
            ids.addAll(listing(request("date-desc", page)).getContent().stream().map(PaperListItem::id).toList());
        }
        assertThat(ids).hasSize(7).doesNotHaveDuplicates().isSortedAccordingTo(java.util.Comparator.reverseOrder());
    }

    private MvcResult request(String order, int page) throws Exception {
        var request = get("/papers").with(user(principal)).param("q", keyword).param("tag", keyword)
                .param("folder", folderId.toString()).param("page", String.valueOf(page)).param("size", "2");
        if (order != null) request.param("order", order);
        return mvc.perform(request).andExpect(status().isOk()).andReturn();
    }

    @SuppressWarnings("unchecked")
    private Page<PaperListItem> listing(MvcResult result) {
        return (Page<PaperListItem>) result.getModelAndView().getModel().get("papers");
    }
}
