package jenkins.plugins.logstash.utils;

import java.net.URI;

import org.apache.commons.beanutils.converters.AbstractConverter;

public class URIConverter extends AbstractConverter {

  @Override
  protected Object convertToType(Class type, Object value) throws Throwable {
    return new URI(value.toString());
  }

  @Override
  protected Class getDefaultType() {
    return URI.class;
  }
}
