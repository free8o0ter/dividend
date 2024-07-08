package com.zerobase.dividend.scraper;

import com.zerobase.dividend.model.Company;
import com.zerobase.dividend.model.Dividend;
import com.zerobase.dividend.model.ScrapedResult;
import com.zerobase.dividend.model.constant.Month;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class YahooFinanceScraper  implements Scraper{

    private static final String URL = "https://finance.yahoo.com/quote/%s/history/?frequency=1mo&period1=%d&period2=%d";
    private static final String SUMMARY_URL = "https://finance.yahoo.com/quote/%s?p=%s";
    private static final long START_TIME = 86400;


    @Override
    public ScrapedResult scrap(Company company){
        ScrapedResult scrapedResult = new ScrapedResult();
        scrapedResult.setCompany(company);

        try{
            long now = System.currentTimeMillis()/1000;
            String url = String.format(URL, company.getTicker(), START_TIME, now);
            Connection connection = Jsoup.connect(url);
            Document document = connection.get();
            Elements parsingDivs = document.getElementsByClass("table svelte-ewueuo");
            Element tableElement = parsingDivs.get(0);

            Element tbody = tableElement.children().get(1);

            List<Dividend> dividends = new ArrayList<>();
            for(Element e : tbody.children()){
                String txt = e.text();
                if(!txt.endsWith("Dividend")){
                    continue;
                }

                String[] splits = txt.split(" ");
                int month = Month.strToNumber(splits[0]);
                int day = Integer.valueOf(splits[1].replace(",",""));
                int year = Integer.valueOf(splits[2]);
                String dividend = splits[3];

                if(month < 0){
                    throw new RuntimeException("Undefined Month enum type => " + splits[0] );
                }

                dividends.add(Dividend.builder()
                        .date(LocalDateTime.of(year, month, day, 0, 0))
                        .dividend(dividend)
                        .build());


            }
            scrapedResult.setDividends(dividends);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return scrapedResult;
    }

    @Override
    public Company scrapCompanyByTicker(String ticker){
        String url = String.format(SUMMARY_URL, ticker, ticker);

        try{
            Document document = Jsoup.connect(url).get();
            Element titleEle = document.getElementsByClass("svelte-3a2v0c").get(0);
            String title = titleEle.text().split("\\(")[0].trim();

            return Company.builder()
                    .ticker(ticker)
                    .name(title)
                    .build();

        } catch (IOException e){
            e.printStackTrace();
        }

        return null;
    }
}
