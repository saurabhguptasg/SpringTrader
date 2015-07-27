package io.pivotal.web.service;

import java.util.*;
import java.util.stream.Collectors;

import io.pivotal.web.domain.*;
import io.pivotal.web.exception.OrderNotSavedException;

import io.pivotal.web.exception.PortfolioException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Service
@EnableScheduling
@RefreshScope
public class MarketService {
	private static final Logger logger = LoggerFactory
			.getLogger(MarketService.class);
	private final static Integer QUOTES_NUMBER = 3;
	
	//10 minutes in milliseconds
	private final static long REFRESH_PERIOD = 600000l;
	
	@Autowired
	@LoadBalanced
	private RestTemplate restTemplate;

  private final Random random = new Random();

  @Value("${latency}")
  Boolean latency;

  private static List<String> symbolsIT = Arrays.asList("EMC", "ORCL", "IBM", "INTC", "AMD", "HPQ", "CSCO", "AAPL");
	private static List<String> symbolsFS = Arrays.asList("JPM", "C", "MS", "BAC", "GS", "WFC","BK");
	
	private MarketSummary summary = new MarketSummary();
	
	public MarketSummary getMarketSummary() {
		logger.debug("Retrieving Market Summary");
		
		return summary;
	}
	
	@HystrixCommand(fallbackMethod = "getQuoteFallback")
	public Quote getQuote(String symbol) {
		logger.debug("Fetching quote: " + symbol);
    if(random.nextBoolean()) {
      logger.debug("|||||||| slowing down quotes |||||||||");
      try {
        Thread.currentThread().wait(10000); //10 second delay
      }
      catch (InterruptedException e) {
        logger.error(e.getMessage(), e);
      }
    }
		Quote quote = restTemplate.getForObject("http://quotes/quote/{symbol}", Quote.class, symbol);
		return quote;
	}
	
	private Quote getQuoteFallback(String symbol) {
		logger.debug("Fetching fallback quote for: " + symbol);
		//Quote quote = restTemplate.getForObject("http://quotes/quote/{symbol}", Quote.class, symbol);
		Quote quote = new Quote();
		quote.setSymbol(symbol);
		quote.setStatus("FAILED");
		return quote;
	}
	@HystrixCommand(fallbackMethod = "getCompaniesFallback")
	public List<CompanyInfo> getCompanies(String name) {
		logger.debug("Fetching companies with name or symbol matching: " + name);
		CompanyInfo[] infos = restTemplate.getForObject("http://quotes/company/{name}", CompanyInfo[].class, name);

		return Arrays.asList(infos);
	}

	private List<CompanyInfo> getCompaniesFallback(String name) {
		List<CompanyInfo> infos = new ArrayList<>();
		return infos;
	}

	@HystrixCommand(fallbackMethod = "getPortfolioValueFallback")
	public PortfolioValue getPortfolioValue(String accountId) {
		ResponseEntity<PortfolioValue>  result = restTemplate.getForEntity("http://portfolio/calc/{accountId}", PortfolioValue.class, accountId);
		if (result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new PortfolioException("Could not get portfolio value");
		}
		logger.debug("portfolio value is:: " + result.getBody());
		return result.getBody();
	}

  public PortfolioValue getPortfolioValueFallback(String accountId) {
    return new PortfolioValue();
  }
	
	
	
	public Order sendOrder(Order order ) throws OrderNotSavedException{
		logger.debug("send order: " + order);
		
		//check result of http request to ensure its ok.
		
		ResponseEntity<Order>  result = restTemplate.postForEntity("http://portfolio/portfolio/{accountId}", order, Order.class, order.getAccountId());
		if (result.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
			throw new OrderNotSavedException("Could not save the order");
		}
		logger.debug("Order saved:: " + result.getBody());
		return result.getBody();
	}


	public Portfolio getPortfolio(String accountId) {
		Portfolio folio = restTemplate.getForObject("http://portfolio/portfolio/{accountid}", Portfolio.class, accountId);
		logger.debug("Portfolio received: " + folio);
		return folio;
	}
	
	//TODO: prime location for a redis/gemfire caching service!
	@Scheduled(fixedRate = REFRESH_PERIOD)
	protected void retrieveMarketSummary() {
		logger.debug("Scheduled retrieval of Market Summary");
		List<Quote> quotesIT = pickRandomThree(symbolsIT).parallelStream().map(symbol -> getQuote(symbol)).collect(Collectors.toList());
		List<Quote> quotesFS = pickRandomThree(symbolsFS).parallelStream().map(symbol -> getQuote(symbol)).collect(Collectors.toList());
		summary.setTopGainers(quotesIT);
		summary.setTopLosers(quotesFS);
	}
	
	private List<String> pickRandomThree(List<String> symbols) {
		List<String> list = new ArrayList<>();
		Collections.shuffle(symbols);
	    list = symbols.subList(0, QUOTES_NUMBER);
	    return list;
	}
}
