import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lectures {
  public static void main(String[] args) throws IOException {

    /**
     ********** 1) 웹 드라이버 세팅
     * */
    System.setProperty("webdriver.chrome.driver", "selenium\\src\\main\\resources\\chromedriver.exe"); // 웹 드라이버 종류, 웹 드라이버 경로 세팅
    WebDriver driver = new ChromeDriver();
    WebDriverWait wait = new WebDriverWait(driver, 5);

    /**
     ********** 2) 초기화면 접속 및 로그인(개인정보 사전 세팅 필요)
     * */
    String login = "https://ucampus.knou.ac.kr/ekp/user/login/retrieveULOLogin.do"; // 초기화면 접속
    driver.get(login);

    Identification identification = new Identification(); // 개인정보 불러오기
    driver.findElement(By.xpath("//*[@id=\"username\"]")).sendKeys(identification.getId());
    driver.findElement(By.xpath("//*[@id=\"password\"]")).sendKeys(identification.getPw());
    driver.findElement(By.xpath("//*[@id=\"loginTab\"]/fieldset/div[4]/button")).click(); // 로그인

    /**
     ********** 3) 강의에 대한 대주제 페이지로 접속할 강의 코드 추출
     * */
    List<Map<String, String>> list = new ArrayList<>();
    wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//*[@id=\"div_srchType03\"]"))); // 화면이 그려질 때까지 기다림
    for (int i = 1; i < 9; i++) { // 총 8개 학기로 이루어져 있으므로 8번 반복한다(고정)
      WebElement grade = driver.findElement(By.xpath("//*[@id=\"div_srchType03\"]/a[" + i + "]")); // 하단 n학년 n학기 차례로 클릭
      grade.click();

      wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("post-list-item"))); // 화면이 그려질 때까지 기다림

      int lectureCount = driver.findElements(By.className("post-list-item")).size(); // 학기당 강의의 개수

      wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("post-list-item"))); // 화면이 그려질 때까지 기다림

      for (int j = 1; j < lectureCount + 1; j++) {
        WebElement lecture = driver.findElement(By.xpath("//*[@id=\"deptCntsList\"]/li[" + j + "]/div")); // 강의 정보(코드, 하위코드, 제목) 패턴화 해서 가져오기
        Pattern pattern = Pattern.compile("<a href=\"javascript:void.+;\" onclick=\"fnCoursePage.+, '(.+?)', '(.+?)'.+<div class=\"post-tb\">.+<img src=.+alt=\"(.+?)\"");
        String lectureSource = lecture.getAttribute("innerHTML");
        Matcher matcher = pattern.matcher(lectureSource);
        while (matcher.find()) {
          Map<String, String> map = new HashMap<>();
          map.put("codeDetail", matcher.group(1));
          map.put("code", matcher.group(2));
          map.put("title", matcher.group(3));
          list.add(map);
        }
      }
    }

    /**
     ********** 4-1) 강의를 저장할 디렉토리 생성
     ********** 4-2) 강의 리스트, 텍스트 파일을 저장할 변수 생성
     * */
    List<Map<String, String>> mp4 = new ArrayList(); // 강의 리스트
    File downloadPath = new File("C:\\KNOU" + System.currentTimeMillis());
    if (!downloadPath.exists()) { // 강의를 저장할 폴더 생성
      downloadPath.mkdir();
    }
    String urlList = ""; // 강의 제목, url 리스트
    String totalList = ""; // 강의 제목, url, html 소스 모든 정보 리스트

    /**
     ********** 5) 3에서 추출한 코드를 차례대로 url로 만든 다음 해당 강의에 포함된 영상 주소들을 모두 리스트화
     * */
    for (Map<String, String> info : list) {
      String lectures = "https://ucampus.knou.ac.kr/ekp/user/course/initUCRCourse.sdo?cntsId=" + info.get("code") + "&sbjtId=" + info.get("codeDetail") + "&tabNo=01"; // 해당 url에 대한 다운로드 수행
      driver.get(lectures);

      WebElement lectureFront = driver.findElement(By.xpath("//*[@id=\"tabMenu\"]/div/div/div/div[1]/ul/li[2]/a")); // 강의목차
      lectureFront.click();

      int watchCount = driver.findElements(By.className("lecture-content-item")).size(); // 하나의 주제에 대한 하위 강의 개수
      int cnt = 1;
      for (int j = 0; j < watchCount; j++) {
        WebElement watchLecture = driver.findElement(By.xpath("//*[@id=\"sequens" + j + "\"]")); // 강의보기
        watchLecture.click();

        // 현재 창 정보 저장
        String winHandleBefore = driver.getWindowHandle();
        // 새창 조작
        for (String winHandle : driver.getWindowHandles()) {
          driver.switchTo().window(winHandle).getCurrentUrl();
        }

        String htmlLecture = driver.getPageSource(); // 영상 스크립트 추출
        new FileOutputStream(downloadPath + "\\" + info.get("title") + "_" + cnt + ".html").write(htmlLecture.getBytes()); // 소스 html 저장(중복 시 덮어씀)

        driver.close(); // 새창 종료
        driver.switchTo().window(winHandleBefore); // 원래 창으로 전환

        // 영상 주소 추출
        Pattern pattern = Pattern.compile("httpUrl.+:.+https://sdn.knou.ac.kr/[A-Za-z].+/[0-9].+/[A-Za-z0-9].+H.mp4");
        Matcher matcher = pattern.matcher(htmlLecture);
        int innerCnt = 1;
        while (matcher.find()) {
          Map<String, String> map = new HashMap();
          map.put("title", info.get("title") + "_" + cnt + "_" + innerCnt++);
          map.put("url", matcher.group().replace("httpUrl\"   : \"", ""));
          map.put("html", htmlLecture);
          mp4.add(map);
          System.out.println("{ 'title' : '" + map.get("title") + "', 'url' : '" + map.get("url") + "' }, "); // 강의 주소 출력 json
          urlList += "{ 'title' : '" + map.get("title") + "', 'url' : '" + map.get("url") + "' }, ";
          totalList += "{ 'title' : '" + map.get("title") + "', 'url' : '" + map.get("url") + "', 'html': '" + map.get("html") + "' }, ";
        }
        cnt++;
      }
    }

    driver.close(); // 웹 작업 종료, 크롬 닫음

    /**
     ********** 6) 영상 주소 및 전반적 정보들을 json 형식으로 text 저장
     * */
    new FileOutputStream(downloadPath + "\\urlList.txt").write(urlList.getBytes()); // 강의 제목, url 리스트 text 저장
    new FileOutputStream(downloadPath + "\\totalList.txt").write(totalList.getBytes()); // 강의 제목, url, html 소스 모든 정보 리스트 text 저장

    /**
     ********** 7) url로 접속해 mp4형식으로 된 영상을 차례대로 다운받음
     * */
    for (Map<String, String> result : mp4) {
      String FILE_PATH = result.get("url");  // 주소 입력
      System.out.println(result.get("title") + " 다운로드 진행중...");
      try {
        // file 저장
        File fileName = new File(downloadPath + "\\" + result.get("title") + ".mp4");
        URL website = new URL(FILE_PATH);
        ReadableByteChannel rbc = Channels.newChannel(website.openStream());
        FileOutputStream fos = new FileOutputStream(fileName);

        fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);  // 처음부터 끝까지 다운로드
        fos.close();
        System.out.println(result.get("title") + " 다운로드 완료");

      } catch (Exception e) {
        e.printStackTrace();
        System.out.println(result.get("title") + " 다운로드 실패");
      }
    }
  }
}
