package com.zerobase.dividend.scheduler;

import com.zerobase.dividend.model.Company;
import com.zerobase.dividend.model.ScrapedResult;
import com.zerobase.dividend.model.constant.CacheKey;
import com.zerobase.dividend.persist.CompanyRepository;
import com.zerobase.dividend.persist.DividendRepository;
import com.zerobase.dividend.persist.entity.CompanyEntity;
import com.zerobase.dividend.persist.entity.DividendEntity;
import com.zerobase.dividend.scraper.Scraper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@EnableCaching
@AllArgsConstructor
public class ScraperScheduler {
    private final CompanyRepository companyRepository;
    private final Scraper yahooFinanceScraper;
    private final DividendRepository dividendRepository;

    @CacheEvict(value = CacheKey.KEY_FINANCE, allEntries = true)
    @Scheduled(cron="${scheduler.scrap.yahoo}")
    public void yahooFinanceScheduling() {
        log.info("scraping scheduler is started");
        //저장된 회사 목록을 조회
        List<CompanyEntity> companys = this.companyRepository.findAll();
        //회사마다 배당금 정보를 새로 스크래핑
        for(CompanyEntity company : companys){
            log.info("scraping schedulrer is started -> " + company.getName());
            ScrapedResult scrapResult = this.yahooFinanceScraper.scrap(Company.builder()
                    .name(company.getName())
                    .ticker(company.getTicker())
                    .build());
            //스크래핑한 배당금 정보 중 데이터베이스에 없는 값 저장
            scrapResult.getDividends().stream()
                    .map(e-> new DividendEntity(company.getId(), e))
                    .forEach( e -> {
                        if(!this.dividendRepository.existsByCompanyIdAndDate(e.getCompanyId(), e.getDate())){
                            this.dividendRepository.save(e);
                        }
                    });
            //연속적으로 스크래핑 대상 사이트 서버에 요청을 날리지 않도록 일시 정지
            try{
                Thread.sleep(3000); //3 second
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }


        }

    }
}
