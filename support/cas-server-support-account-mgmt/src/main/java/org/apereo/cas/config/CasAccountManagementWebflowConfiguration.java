package org.apereo.cas.config;

import org.apereo.cas.acct.AccountRegistrationPropertyLoader;
import org.apereo.cas.acct.AccountRegistrationService;
import org.apereo.cas.acct.AccountRegistrationTokenCipherExecutor;
import org.apereo.cas.acct.DefaultAccountRegistrationPropertyLoader;
import org.apereo.cas.acct.DefaultAccountRegistrationService;
import org.apereo.cas.acct.webflow.AccountManagementWebflowConfigurer;
import org.apereo.cas.acct.webflow.LoadAccountRegistrationPropertiesAction;
import org.apereo.cas.acct.webflow.SubmitAccountRegistrationAction;
import org.apereo.cas.configuration.CasConfigurationProperties;
import org.apereo.cas.notifications.CommunicationsManager;
import org.apereo.cas.ticket.TicketFactory;
import org.apereo.cas.ticket.registry.TicketRegistry;
import org.apereo.cas.util.cipher.CipherExecutorUtils;
import org.apereo.cas.util.crypto.CipherExecutor;
import org.apereo.cas.web.flow.CasWebflowConfigurer;
import org.apereo.cas.web.flow.CasWebflowExecutionPlanConfigurer;

import lombok.val;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.webflow.definition.registry.FlowDefinitionRegistry;
import org.springframework.webflow.engine.builder.support.FlowBuilderServices;
import org.springframework.webflow.execution.Action;

/**
 * This is {@link CasAccountManagementWebflowConfiguration}.
 *
 * @author Misagh Moayyed
 * @since 6.5.0
 */
@Configuration(value = "CasAccountManagementWebflowConfiguration")
@EnableConfigurationProperties(CasConfigurationProperties.class)
public class CasAccountManagementWebflowConfiguration {
    @Autowired
    @Qualifier("defaultTicketFactory")
    private ObjectProvider<TicketFactory> defaultTicketFactory;

    @Autowired
    @Qualifier("loginFlowRegistry")
    private ObjectProvider<FlowDefinitionRegistry> loginFlowDefinitionRegistry;

    @Autowired
    private CasConfigurationProperties casProperties;

    @Autowired
    @Qualifier("flowBuilderServices")
    private ObjectProvider<FlowBuilderServices> flowBuilderServices;

    @Autowired
    private ConfigurableApplicationContext applicationContext;

    @Autowired
    @Qualifier("ticketRegistry")
    private ObjectProvider<TicketRegistry> ticketRegistry;

    @Autowired
    @Qualifier("communicationsManager")
    private ObjectProvider<CommunicationsManager> communicationsManager;

    @ConditionalOnMissingBean(name = "accountMgmtWebflowConfigurer")
    @Bean
    @DependsOn("defaultWebflowConfigurer")
    public CasWebflowConfigurer accountMgmtWebflowConfigurer() {
        return new AccountManagementWebflowConfigurer(flowBuilderServices.getObject(),
            loginFlowDefinitionRegistry.getObject(), applicationContext, casProperties);
    }

    @Bean
    @ConditionalOnMissingBean(name = "accountMgmtCasWebflowExecutionPlanConfigurer")
    public CasWebflowExecutionPlanConfigurer accountMgmtCasWebflowExecutionPlanConfigurer() {
        return plan -> plan.registerWebflowConfigurer(accountMgmtWebflowConfigurer());
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "loadAccountRegistrationPropertiesAction")
    public Action loadAccountRegistrationPropertiesAction() {
        return new LoadAccountRegistrationPropertiesAction(accountMgmtRegistrationService());
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "submitAccountRegistrationAction")
    public Action submitAccountRegistrationAction() {
        return new SubmitAccountRegistrationAction(accountMgmtRegistrationService(), casProperties,
            communicationsManager.getObject(), defaultTicketFactory.getObject(), ticketRegistry.getObject());
    }

    @ConditionalOnMissingBean(name = "accountMgmtCipherExecutor")
    @RefreshScope
    @Bean
    public CipherExecutor accountMgmtCipherExecutor() {
        val crypto = casProperties.getAccountRegistration().getCore().getCrypto();
        return crypto.isEnabled()
            ? CipherExecutorUtils.newStringCipherExecutor(crypto, AccountRegistrationTokenCipherExecutor.class)
            : CipherExecutor.noOp();
    }

    @Bean
    @RefreshScope
    @ConditionalOnMissingBean(name = "accountMgmtRegistrationService")
    public AccountRegistrationService accountMgmtRegistrationService() {
        return new DefaultAccountRegistrationService(accountMgmtRegistrationPropertyLoader(),
            casProperties, accountMgmtCipherExecutor());
    }

    @Bean
    @ConditionalOnMissingBean(name = "accountMgmtRegistrationPropertyLoader")
    @RefreshScope
    public AccountRegistrationPropertyLoader accountMgmtRegistrationPropertyLoader() {
        val resource = casProperties.getAccountRegistration().getCore().getRegistrationProperties().getLocation();
        return new DefaultAccountRegistrationPropertyLoader(resource);
    }
}