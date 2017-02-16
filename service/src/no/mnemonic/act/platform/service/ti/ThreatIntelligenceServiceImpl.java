package no.mnemonic.act.platform.service.ti;

import no.mnemonic.act.platform.api.exceptions.AccessDeniedException;
import no.mnemonic.act.platform.api.exceptions.AuthenticationFailedException;
import no.mnemonic.act.platform.api.exceptions.InvalidArgumentException;
import no.mnemonic.act.platform.api.exceptions.ObjectNotFoundException;
import no.mnemonic.act.platform.api.model.v1.*;
import no.mnemonic.act.platform.api.model.v1.Object;
import no.mnemonic.act.platform.api.request.v1.*;
import no.mnemonic.act.platform.api.service.v1.RequestHeader;
import no.mnemonic.act.platform.api.service.v1.ResultSet;
import no.mnemonic.act.platform.api.service.v1.ThreatIntelligenceService;
import no.mnemonic.act.platform.dao.cassandra.FactManager;
import no.mnemonic.act.platform.dao.cassandra.ObjectManager;
import no.mnemonic.act.platform.entity.cassandra.FactEntity;
import no.mnemonic.act.platform.entity.handlers.EntityHandlerFactory;
import no.mnemonic.act.platform.service.Service;
import no.mnemonic.act.platform.service.contexts.RequestContext;
import no.mnemonic.act.platform.service.contexts.SecurityContext;
import no.mnemonic.act.platform.service.ti.converters.FactConverter;
import no.mnemonic.act.platform.service.ti.converters.FactTypeConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectConverter;
import no.mnemonic.act.platform.service.ti.converters.ObjectTypeConverter;
import no.mnemonic.act.platform.service.ti.delegates.*;
import no.mnemonic.act.platform.service.validators.ValidatorFactory;
import no.mnemonic.commons.utilities.ObjectUtils;

import javax.inject.Inject;
import java.util.UUID;
import java.util.function.Function;

public class ThreatIntelligenceServiceImpl implements Service, ThreatIntelligenceService {

  public static final UUID GLOBAL_NAMESPACE = UUID.fromString("00000000-0000-0000-0000-000000000000");

  private final FactManager factManager;
  private final ObjectManager objectManager;
  private final EntityHandlerFactory entityHandlerFactory;
  private final ValidatorFactory validatorFactory;
  private final ObjectTypeConverter objectTypeConverter;
  private final FactTypeConverter factTypeConverter;
  private final ObjectConverter objectConverter;
  private final FactConverter factConverter;

  @Inject
  public ThreatIntelligenceServiceImpl(FactManager factManager, ObjectManager objectManager,
                                       EntityHandlerFactory entityHandlerFactory, ValidatorFactory validatorFactory) {
    this.factManager = factManager;
    this.objectManager = objectManager;
    this.entityHandlerFactory = entityHandlerFactory;
    this.validatorFactory = validatorFactory;
    this.objectTypeConverter = ObjectTypeConverter.builder()
            .setNamespaceConverter(createNamespaceConverter())
            .build();
    this.factTypeConverter = FactTypeConverter.builder()
            .setNamespaceConverter(createNamespaceConverter())
            .setObjectTypeConverter(createObjectTypeByIdConverter())
            .build();
    this.objectConverter = ObjectConverter.builder()
            .setObjectTypeConverter(createObjectTypeByIdConverter())
            .build();
    this.factConverter = FactConverter.builder()
            .setFactTypeConverter(createFactTypeByIdConverter())
            .setInReferenceToConverter(createInReferenceToConverter())
            .setOrganizationConverter(createOrganizationConverter())
            .setSourceConverter(createSourceConverter())
            .setObjectConverter(createObjectByIdConverter())
            .build();
  }

  @Override
  public SecurityContext createSecurityContext() {
    return TiSecurityContext.builder()
            .setAclResolver(factManager::fetchFactAcl)
            .build();
  }

  @Override
  public RequestContext createRequestContext() {
    return TiRequestContext.builder()
            .setFactManager(factManager)
            .setObjectManager(objectManager)
            .setEntityHandlerFactory(entityHandlerFactory)
            .setValidatorFactory(validatorFactory)
            .setObjectTypeConverter(objectTypeConverter)
            .setFactTypeConverter(factTypeConverter)
            .setObjectConverter(objectConverter)
            .setFactConverter(factConverter)
            .build();
  }

  @Override
  public ObjectType getObjectType(RequestHeader rh, GetObjectTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ObjectTypeGetByIdDelegate.create().handle(request);
  }

  @Override
  public ResultSet<ObjectType> searchObjectTypes(RequestHeader rh, SearchObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ObjectTypeSearchDelegate.create().handle(request);
  }

  @Override
  public ObjectType createObjectType(RequestHeader rh, CreateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return ObjectTypeCreateDelegate.create().handle(request);
  }

  @Override
  public ObjectType updateObjectType(RequestHeader rh, UpdateObjectTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return ObjectTypeUpdateDelegate.create().handle(request);
  }

  @Override
  public FactType getFactType(RequestHeader rh, GetFactTypeByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return FactTypeGetByIdDelegate.create().handle(request);
  }

  @Override
  public ResultSet<FactType> searchFactTypes(RequestHeader rh, SearchFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return FactTypeSearchDelegate.create().handle(request);
  }

  @Override
  public FactType createFactType(RequestHeader rh, CreateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException {
    return FactTypeCreateDelegate.create().handle(request);
  }

  @Override
  public FactType updateFactType(RequestHeader rh, UpdateFactTypeRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return FactTypeUpdateDelegate.create().handle(request);
  }

  @Override
  public Fact getFact(RequestHeader rh, GetFactByIdRequest request)
          throws AccessDeniedException, AuthenticationFailedException, InvalidArgumentException, ObjectNotFoundException {
    return FactGetByIdDelegate.create().handle(request);
  }

  private Function<UUID, Namespace> createNamespaceConverter() {
    // For now everything will just be part of the global namespace.
    return id -> Namespace.builder()
            .setId(GLOBAL_NAMESPACE)
            .setName("Global")
            .build();
  }

  private Function<UUID, Organization> createOrganizationConverter() {
    // For now just return a static Organization.
    return id -> Organization.builder()
            .setId(id)
            .setName("Not implemented yet!")
            .build();
  }

  private Function<UUID, Source> createSourceConverter() {
    // For now just return a static Source.
    return id -> Source.builder()
            .setId(id)
            .setName("Not implemented yet!")
            .build();
  }

  private Function<UUID, ObjectType> createObjectTypeByIdConverter() {
    return id -> ObjectUtils.ifNotNull(objectManager.getObjectType(id), objectTypeConverter, ObjectType.builder().setId(id).setName("N/A").build());
  }

  private Function<UUID, FactType> createFactTypeByIdConverter() {
    return id -> ObjectUtils.ifNotNull(factManager.getFactType(id), factTypeConverter, FactType.builder().setId(id).setName("N/A").build());
  }

  private Function<UUID, Object> createObjectByIdConverter() {
    return id -> ObjectUtils.ifNotNull(objectManager.getObject(id), objectConverter, Object.builder().setId(id).setValue("N/A").build());
  }

  private Function<UUID, Fact> createInReferenceToConverter() {
    return id -> {
      FactEntity inReferenceTo = factManager.getFact(id);
      if (inReferenceTo == null) return null;
      // Assume that access to 'inReferenceTo' Fact was already verified.
      // Also, avoid resolving recursive 'inReferenceTo' Facts. Clone entity first in order to not disturb DAO layer.
      return factConverter.apply(inReferenceTo.clone().setInReferenceToID(null));
    };
  }

}