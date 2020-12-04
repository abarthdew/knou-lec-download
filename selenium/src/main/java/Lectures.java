import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.awt.*;
import java.awt.event.InputEvent;
import java.io.*;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Lectures {
    public static void main(String[] args) throws InterruptedException, AWTException, IOException {

        System.setProperty("webdriver.chrome.driver", "selenium\\src\\main\\resources\\chromedriver.exe");
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
        File downloadPath = new File("C:\\" + System.currentTimeMillis());
        if(!downloadPath.exists()) {// 폴더 생성
            downloadPath.mkdir();
        }
        String urlList = "";
        String totalList = "";

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
                new FileOutputStream(downloadPath + "\\"+ m.get("title") + "_" + cnt + ".html").write(htmlLecture.getBytes()); // html 저장(중복 시 덮어씀)

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
                    urlList += "{ 'title' : '" + map.get("title") + "', 'url' : '" + map.get("url") + "' }, ";
                    totalList += "{ 'title' : '" + map.get("title") + "', 'url' : '" + map.get("url") + "', 'html': '" + map.get("html") + "' }, ";
                }
                cnt++;
            }
        }

        new FileOutputStream(downloadPath + "\\urlList.txt").write(urlList.getBytes());
        new FileOutputStream(downloadPath + "\\totalList.txt").write(totalList.getBytes());

//        List<Map<String, String>> result = distinctArray(mp4, "name"); // 영상주소 중복 제거
        List<Map<String, String>> result = new ArrayList<>(); // 영상주소 중복 제거
        mp4.stream()
                .distinct() // 영상 주소 중복 제거
                .forEach(result::add);
//        System.out.println(result); // 최종 url

        for (Map<String, String> rs : result) {
            String FILE_PATH = rs.get("url");  // 주소 입력
            System.out.println(rs.get("title") + " 다운로드 진행중...");
            try {
                // html 저장
//                OutputStream output = new FileOutputStream("C:\\tmp\\" + rs.get("title") + ".html");
//                String html = rs.get("html");
//                byte[] by = html.getBytes();
//                output.write(by);

                // file 저장
                File fileName = new File(downloadPath + "\\" + rs.get("title") + ".mp4");
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


    //중복제거 메소드, key는 제거할 맵 대상
//    private static List<Map<String, String>> distinctArray(List<Map<String, String>> target, String key){
//        if(target != null){
//            target = target.stream().filter(distinctByKey(o-> o.get(key))).collect(Collectors.toList());
//        }
//        return target;
//    }
//
//    //중복 제거를 위한 함수
//    private static <T> Predicate<T> distinctByKey(Function<? super T, String> keyExtractor) {
//        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
//        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
//    }
}
