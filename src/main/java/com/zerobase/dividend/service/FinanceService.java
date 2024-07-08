package com.zerobase.dividend.service;

import com.zerobase.dividend.model.Company;
import com.zerobase.dividend.model.Dividend;
import com.zerobase.dividend.model.ScrapedResult;
import com.zerobase.dividend.model.constant.CacheKey;
import com.zerobase.dividend.persist.CompanyRepository;
import com.zerobase.dividend.persist.DividendRepository;
import com.zerobase.dividend.persist.entity.CompanyEntity;
import com.zerobase.dividend.persist.entity.DividendEntity;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class FinanceService {

    private final CompanyRepository companyRepository;
    private final DividendRepository dividendRepository;

    @Cacheable(key="#companyName", value = CacheKey.KEY_FINANCE)
    public ScrapedResult getDividendByCompanyName(String companyName){
        log.info("serch Company");

        // 회사명으로 회사 정보 조회
        CompanyEntity company = this.companyRepository.findByName(companyName)
                                    .orElseThrow(()-> new RuntimeException("failed find companyname"));

        // 조회된 회사의 Id로 배당금 정보 조회
        List<DividendEntity> dividendEntities = this.dividendRepository.findAllByCompanyId(company.getId());


        // 결과 조합 후 변환
        List<Dividend> dividends = new ArrayList<>();
        for(DividendEntity d : dividendEntities){
            dividends.add(Dividend.builder()
                    .date(d.getDate())
                    .dividend(d.getDividend())
                    .build());
        }


        return new ScrapedResult(Company.builder()
                                            .ticker(company.getTicker())
                                            .name(company.getName())
                                            .build(),
                                 dividends
                );
    }
}
