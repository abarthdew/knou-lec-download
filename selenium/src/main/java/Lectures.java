import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lectures {
    public static void main(String[] args) throws InterruptedException, AWTException {

        System.setProperty("webdriver.chrome.driver", "C:\\dmsales\\workspace\\knou\\selenium\\src\\main\\resources\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        String login = "https://ucampus.knou.ac.kr/ekp/user/login/retrieveULOLogin.do"; // 초기화면 접속
        driver.get(login);

        Identification identification = new Identification(); // 개인정보 불러오기
        driver.findElement(By.xpath("//*[@id=\"username\"]")).sendKeys( identification.getId());
        driver.findElement(By.xpath("//*[@id=\"password\"]")).sendKeys(identification.getPw());
        driver.findElement(By.xpath("//*[@id=\"loginTab\"]/fieldset/div[4]/button")).click(); // 로그인

        List<Map<String, String>> list = new ArrayList<>();
        for (int i=1; i<9; i++) {
            WebElement watchLecture = driver.findElement(By.xpath("//*[@id=\"div_srchType03\"]/a[" + i + "]")); // 한 학년씩 클릭
            watchLecture.click();

            int lectureCount = driver.findElements(By.className("post-list-item")).size();
            for (int j=1; j<lectureCount+1; j++) {
                WebElement w = driver.findElement(By.xpath("//*[@id=\"deptCntsList\"]/li[" + j + "]/div"));
                Pattern pattern = Pattern.compile("<a href=\"javascript:void.+;\" onclick=\"fnCoursePage.+, '(.+?)', '(.+?)'.+<div class=\"post-tb\">.+<img src=.+alt=\"(.+?)\"");
                String thisSource = w.getAttribute("innerHTML");
                Matcher address = pattern.matcher(thisSource);
                while(address.find()) {
                    Map<String, String> map = new HashMap<>();
                    map.put("codeDetail", address.group(1));
                    map.put("code", address.group(2));
                    map.put("title", address.group(3));
                    list.add(map);
                }
            }
        }

        List<Map<String, String>> mp4 = new ArrayList();
        for (Map<String, String> m : list) {
            String lectures = "https://ucampus.knou.ac.kr/ekp/user/course/initUCRCourse.sdo?cntsId=" + m.get("code") + "&sbjtId=" + m.get("codeDetail") + "&tabNo=01";
            driver.get(lectures);

            WebElement lectureList = driver.findElement(By.xpath("//*[@id=\"tabMenu\"]/div/div/div/div[1]/ul/li[2]/a")); // 강의목차
            lectureList.click();

            int watchCount = driver.findElements(By.className("lecture-content-item")).size();
            int cnt = 1;
            for (int j=0; j<watchCount; j++) {
                WebElement watchLecture = driver.findElement(By.xpath("//*[@id=\"sequens" + j + "\"]")); // 강의보기
                watchLecture.click();

                // 현재 창 정보 저장
                String winHandleBefore = driver.getWindowHandle();
                // 새창 조작
                for(String winHandle : driver.getWindowHandles()){
                    driver.switchTo().window(winHandle).getCurrentUrl();
                }

                String htmlLecture = driver.getPageSource(); // 영상 스크립트 추출

                driver.close(); // 새창 종료
                driver.switchTo().window(winHandleBefore); // 원래 창으로 전환

                // 영상 주소 추출
                Pattern pattern = Pattern.compile("httpUrl.+:.+https://sdn.knou.ac.kr/[A-Za-z].+/[0-9].+/[A-Za-z0-9].+H.mp4");
                Matcher address = pattern.matcher(htmlLecture);
                int innerCnt = 1;
                while(address.find()) {
                    Map<String, String> map = new HashMap();
                    map.put("title", m.get("title") + "_" + cnt + "_" + innerCnt++);
                    map.put("url", address.group().replace("httpUrl\"   : \"", ""));
                    map.put("html", htmlLecture);
                    mp4.add(map);
                    System.out.println("{ 'title' : " + map.get("title") + ", 'url' : " + map.get("url") + " }, ");
                }
                cnt++;
            }
        }

        List<Map<String, String>> result = new ArrayList<>();
        mp4.stream()
                .distinct() // 영상 주소 중복 제거
                .forEach(result::add);
//        System.out.println(result); // 최종 url

        for (Map<String, String> rs : result) {
            String FILE_PATH = rs.get("url");  // 주소 입력
            System.out.println(rs.get("title") + " 다운로드 진행중...");
            try {
                // text 저장
                OutputStream output = new FileOutputStream("C:\\Users\\Jonah\\jonahswork\\KNOU-lec-download\\" + rs.get("title") + ".html");
                String html = rs.get("html");
                byte[] by = html.getBytes();
                output.write(by);

                // file 저장
                String fileName = rs.get("title") + ".mp4";
                URL website = new URL(FILE_PATH);
                ReadableByteChannel rbc = Channels.newChannel(website.openStream());
                FileOutputStream fos = new FileOutputStream(fileName);

                fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);  // 처음부터 끝까지 다운로드
                fos.close();
                System.out.println(rs.get("title") + " 다운로드 완료");

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println(rs.get("title") + " 다운로드 실패");
            }
        }

        driver.close(); // 작업 종료
    }
}
