/**
 * Copyright (c) 2016-2018 TypeFox and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0,
 * or the Eclipse Distribution License v. 1.0 which is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * 
 * SPDX-License-Identifier: EPL-2.0 OR BSD-3-Clause
 */
package org.eclipse.lsp4j;

import org.eclipse.lsp4j.jsonrpc.ProtocolDraft;
import org.eclipse.lsp4j.jsonrpc.ProtocolSince;
import org.eclipse.lsp4j.jsonrpc.util.ToStringBuilder;

/**
 * Client capabilities specific to diagnostic pull requests.
 */
@ProtocolSince("3.17.0")
@SuppressWarnings("all")
public class DiagnosticCapabilities extends DynamicRegistrationCapabilities {
  /**
   * Whether the client supports related documents for document diagnostic pulls.
   */
  private Boolean relatedDocumentSupport;

  /**
   * Whether the client accepts diagnostics with related information.
   */
  private Boolean relatedInformation;

  /**
   * Whether the client supports the {@link Diagnostic#tags} property.
   * Clients supporting tags have to handle unknown tags gracefully.
   */
  private DiagnosticsTagSupport tagSupport;

  /**
   * Whether the client supports the {@link Diagnostic#codeDescription} property.
   */
  private Boolean codeDescriptionSupport;

  /**
   * Whether the client supports the {@link Diagnostic#data} property.
   */
  private Boolean dataSupport;

  /**
   * Whether the client supports {@link MarkupContent} in diagnostic messages.
   */
  @ProtocolDraft
  @ProtocolSince("3.18.0")
  private Boolean markupMessageSupport;

  public DiagnosticCapabilities() {
  }

  public DiagnosticCapabilities(final Boolean dynamicRegistration) {
    this.setDynamicRegistration(dynamicRegistration);
  }

  public DiagnosticCapabilities(final Boolean dynamicRegistration, final Boolean relatedDocumentSupport) {
    this(dynamicRegistration);
    this.relatedDocumentSupport = relatedDocumentSupport;
  }

  /**
   * Whether the client supports related documents for document diagnostic pulls.
   */
  public Boolean getRelatedDocumentSupport() {
    return this.relatedDocumentSupport;
  }

  /**
   * Whether the client supports related documents for document diagnostic pulls.
   */
  public void setRelatedDocumentSupport(final Boolean relatedDocumentSupport) {
    this.relatedDocumentSupport = relatedDocumentSupport;
  }

  /**
   * Whether the client accepts diagnostics with related information.
   */
  public Boolean getRelatedInformation() {
    return this.relatedInformation;
  }

  /**
   * Whether the client accepts diagnostics with related information.
   */
  public void setRelatedInformation(final Boolean relatedInformation) {
    this.relatedInformation = relatedInformation;
  }

  /**
   * Whether the client supports the {@link Diagnostic#tags} property.
   * Clients supporting tags have to handle unknown tags gracefully.
   */
  public DiagnosticsTagSupport getTagSupport() {
    return this.tagSupport;
  }

  /**
   * Whether the client supports the {@link Diagnostic#tags} property.
   * Clients supporting tags have to handle unknown tags gracefully.
   */
  public void setTagSupport(final DiagnosticsTagSupport tagSupport) {
    this.tagSupport = tagSupport;
  }

  /**
   * Whether the client supports the {@link Diagnostic#codeDescription} property.
   */
  public Boolean getCodeDescriptionSupport() {
    return this.codeDescriptionSupport;
  }

  /**
   * Whether the client supports the {@link Diagnostic#codeDescription} property.
   */
  public void setCodeDescriptionSupport(final Boolean codeDescriptionSupport) {
    this.codeDescriptionSupport = codeDescriptionSupport;
  }

  /**
   * Whether the client supports the {@link Diagnostic#data} property.
   */
  public Boolean getDataSupport() {
    return this.dataSupport;
  }

  /**
   * Whether the client supports the {@link Diagnostic#data} property.
   */
  public void setDataSupport(final Boolean dataSupport) {
    this.dataSupport = dataSupport;
  }

  /**
   * Whether the client supports {@link MarkupContent} in diagnostic messages.
   */
  @ProtocolDraft
  @ProtocolSince("3.18.0")
  public Boolean getMarkupMessageSupport() {
    return this.markupMessageSupport;
  }

  /**
   * Whether the client supports {@link MarkupContent} in diagnostic messages.
   */
  @ProtocolDraft
  @ProtocolSince("3.18.0")
  public void setMarkupMessageSupport(final Boolean markupMessageSupport) {
    this.markupMessageSupport = markupMessageSupport;
  }

  @Override
  public String toString() {
    ToStringBuilder b = new ToStringBuilder(this);
    b.add("relatedDocumentSupport", this.relatedDocumentSupport);
    b.add("relatedInformation", this.relatedInformation);
    b.add("tagSupport", this.tagSupport);
    b.add("codeDescriptionSupport", this.codeDescriptionSupport);
    b.add("dataSupport", this.dataSupport);
    b.add("markupMessageSupport", this.markupMessageSupport);
    b.add("dynamicRegistration", getDynamicRegistration());
    return b.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    if (!super.equals(obj))
      return false;
    DiagnosticCapabilities other = (DiagnosticCapabilities) obj;
    if (this.relatedDocumentSupport == null) {
      if (other.relatedDocumentSupport != null)
        return false;
    } else if (!this.relatedDocumentSupport.equals(other.relatedDocumentSupport))
      return false;
    if (this.relatedInformation == null) {
      if (other.relatedInformation != null)
        return false;
    } else if (!this.relatedInformation.equals(other.relatedInformation))
      return false;
    if (this.tagSupport == null) {
      if (other.tagSupport != null)
        return false;
    } else if (!this.tagSupport.equals(other.tagSupport))
      return false;
    if (this.codeDescriptionSupport == null) {
      if (other.codeDescriptionSupport != null)
        return false;
    } else if (!this.codeDescriptionSupport.equals(other.codeDescriptionSupport))
      return false;
    if (this.dataSupport == null) {
      if (other.dataSupport != null)
        return false;
    } else if (!this.dataSupport.equals(other.dataSupport))
      return false;
    if (this.markupMessageSupport == null) {
      if (other.markupMessageSupport != null)
        return false;
    } else if (!this.markupMessageSupport.equals(other.markupMessageSupport))
      return false;
    return true;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((this.relatedDocumentSupport== null) ? 0 : this.relatedDocumentSupport.hashCode());
    result = prime * result + ((this.relatedInformation== null) ? 0 : this.relatedInformation.hashCode());
    result = prime * result + ((this.tagSupport== null) ? 0 : this.tagSupport.hashCode());
    result = prime * result + ((this.codeDescriptionSupport== null) ? 0 : this.codeDescriptionSupport.hashCode());
    result = prime * result + ((this.dataSupport== null) ? 0 : this.dataSupport.hashCode());
    return prime * result + ((this.markupMessageSupport== null) ? 0 : this.markupMessageSupport.hashCode());
  }
}
