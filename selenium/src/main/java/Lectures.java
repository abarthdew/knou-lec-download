import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;

import java.awt.*;
import java.awt.event.InputEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Lectures {
    public static void main(String[] args) throws InterruptedException, AWTException {

        System.setProperty("webdriver.chrome.driver", "C:\\dmsales\\workspace\\knou\\selenium\\src\\main\\resources\\chromedriver.exe");
        WebDriver driver = new ChromeDriver();

        String login = "https://ucampus.knou.ac.kr/ekp/user/login/retrieveULOLogin.do";
        driver.get(login);
        driver.findElement(By.xpath("//*[@id=\"username\"]")).sendKeys( "");
        driver.findElement(By.xpath("//*[@id=\"password\"]")).sendKeys("");
        driver.findElement(By.xpath("//*[@id=\"loginTab\"]/fieldset/div[4]/button")).click(); // 로그인

//        String lectures = "https://ucampus.knou.ac.kr/ekp/user/course/initUCRCourse.sdo?cntsId=KNOU" + 0065 + "&sbjtId=KNOU" + 0065001 + "&tabNo=01";
        String lectures = "https://ucampus.knou.ac.kr/ekp/user/course/initUCRCourse.sdo?cntsId=KNOU1314&sbjtId=KNOU1314001&tabNo=01";
        driver.get(lectures);

        WebElement lectureList = driver.findElement(By.xpath("//*[@id=\"tabMenu\"]/div/div/div/div[1]/ul/li[2]/a")); // 강의목차
        lectureList.click();

        WebElement watchLecture = driver.findElement(By.xpath("//*[@id=\"sequens0\"]")); // 강의보기
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
        Pattern pattern = Pattern.compile("https://sdn.knou.ac.kr/[A-Za-z].+/[0-9].+/[A-Za-z0-9].+H.mp4");
        Matcher address = pattern.matcher(htmlLecture);
        List<String> mp4 = new ArrayList();
        while(address.find()) {
            mp4.add(address.group());
        }

        mp4.stream()
            .distinct() // 영상 주소 중복 제거
            .forEach(System.out::println);

        WebDriver video = new ChromeDriver();
        video.get(mp4.get(0));

        Robot robot = new Robot();
        robot.mouseMove(500, 500);
        robot.mousePress(InputEvent.BUTTON3_MASK);
        robot.mouseRelease(InputEvent.BUTTON3_MASK);

        video.close(); // 영상 다운로드 후 종료

        driver.close(); // 작업 종료
    }
}
