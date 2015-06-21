package org.docker.hackathon.util;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;
import java.io.IOException;
import java.io.StringReader;

public class ModifiedPomXMLEventReader
    implements XMLEventReader
{

    /**
     * Field MAX_MARKS
     */
    private static final int MAX_MARKS = 3;

    /**
     * Field pom
     */
    private final StringBuilder pom;

    /**
     * Field modified
     */
    private boolean modified = false;

    /**
     * Field factory
     */
    private final XMLInputFactory factory;

    /**
     * Field nextStart
     */
    private int nextStart = 0;

    /**
     * Field nextEnd
     */
    private int nextEnd = 0;

    /**
     * Field markStart
     */
    private int[] markStart = new int[MAX_MARKS];

    /**
     * Field markEnd
     */
    private int[] markEnd = new int[MAX_MARKS];

    /**
     * Field markDelta
     */
    private int[] markDelta = new int[MAX_MARKS];

    /**
     * Field lastStart
     */
    private int lastStart = -1;

    /**
     * Field lastEnd
     */
    private int lastEnd;

    /**
     * Field lastDelta
     */
    private int lastDelta = 0;

    /**
     * Field next
     */
    private XMLEvent next = null;

    /**
     * Field nextDelta
     */
    private int nextDelta = 0;

    /**
     * Field backing
     */
    private XMLEventReader backing;

    /**
     * Constructor ModifiedPomXMLEventReader creates a new ModifiedPomXMLEventReader instance.
     *
     * @param pom     of type StringBuilder
     * @param factory of type XMLInputFactory
     * @throws XMLStreamException when
     */
    public ModifiedPomXMLEventReader( StringBuilder pom, XMLInputFactory factory )
        throws XMLStreamException
    {
        this.pom = pom;
        this.factory = factory;
        rewind();
    }

    /**
     * Rewind to the start so we can run through again.
     *
     * @throws XMLStreamException when things go wrong.
     */
    public void rewind()
        throws XMLStreamException
    {
        backing = factory.createXMLEventReader( new StringReader( pom.toString() ) );
        nextEnd = 0;
        nextDelta = 0;
        for ( int i = 0; i < MAX_MARKS; i++ )
        {
            markStart[i] = -1;
            markEnd[i] = -1;
            markDelta[i] = 0;
        }
        lastStart = -1;
        lastEnd = -1;
        lastDelta = 0;
        next = null;
    }

    /**
     * Getter for property 'modified'.
     *
     * @return Value for property 'modified'.
     */
    public boolean isModified()
    {
        return modified;
    }

    public Object next()
    {
        try
        {
            return nextEvent();
        }
        catch ( XMLStreamException e )
        {
            return null;
        }
    }

    public void remove()
    {
        throw new UnsupportedOperationException();
    }

    public XMLEvent nextEvent()
        throws XMLStreamException
    {
        try
        {
            return next;
        }
        finally
        {
            next = null;
            lastStart = nextStart;
            lastEnd = nextEnd;
            lastDelta = nextDelta;
        }
    }

    public XMLEvent peek()
        throws XMLStreamException
    {
        return backing.peek();
    }

    public String getElementText()
        throws XMLStreamException
    {
        return backing.getElementText();
    }

    public XMLEvent nextTag()
        throws XMLStreamException
    {
        while ( hasNext() )
        {
            XMLEvent e = nextEvent();
            if ( e.isCharacters() && !( (Characters) e ).isWhiteSpace() )
            {
                throw new XMLStreamException( "Unexpected text" );
            }
            if ( e.isStartElement() || e.isEndElement() )
            {
                return e;
            }
        }
        throw new XMLStreamException( "Unexpected end of Document" );
    }

    public Object getProperty( String name )
    {
        return backing.getProperty( name );
    }

    public void close()
        throws XMLStreamException
    {
        backing.close();
        next = null;
        backing = null;
    }

    /**
     * Returns a copy of the backing string buffer.
     *
     * @return a copy of the backing string buffer.
     */
    public StringBuilder asStringBuilder()
    {
        return new StringBuilder( pom.toString() );
    }

    /**
     * Clears the mark.
     *
     * @param index the mark to clear.
     */
    public void clearMark( int index )
    {
        markStart[index] = -1;
    }

    /**
     * the verbatim text of the current element when {@link #mark(int)} was called.
     *
     * @param index The mark index.
     * @return the current element when {@link #mark(int)} was called.
     */
    public String getMarkVerbatim( int index )
    {
        if ( hasMark( index ) )
        {
            return pom.substring( markDelta[index] + markStart[index], markDelta[index] + markEnd[index] );
        }
        return "";
    }

    /**
     * Returns the verbatim text of the element returned by {@link #peek()}.
     *
     * @return the verbatim text of the element returned by {@link #peek()}.
     */
    public String getPeekVerbatim()
    {
        if ( hasNext() )
        {
            return pom.substring( nextDelta + nextStart, nextDelta + nextEnd );
        }
        return "";
    }

    public boolean hasNext()
    {
        if ( next != null )
        {
            // fast path
            return true;
        }
        if ( !backing.hasNext() )
        {
            // fast path
            return false;
        }
        try
        {
            next = backing.nextEvent();
            nextStart = nextEnd;
            if ( backing.hasNext() )
            {
                nextEnd = backing.peek().getLocation().getCharacterOffset();
            }

            if ( nextEnd != -1 )
            {
                if ( !next.isCharacters() )
                {
                    while ( nextStart < nextEnd && nextStart < pom.length() &&
                        ( c( nextStart ) == '\n' || c( nextStart ) == '\r' ) )
                    {
                        nextStart++;
                    }
                }
                else
                {
                    while ( nextEndIncludesNextEvent() || nextEndIncludesNextEndElement() )
                    {
                        nextEnd--;
                    }
                }
            }
            return nextStart < pom.length();
        }
        catch ( XMLStreamException e )
        {
            return false;
        }
    }

    /**
     * Getter for property 'verbatim'.
     *
     * @return Value for property 'verbatim'.
     */
    public String getVerbatim()
    {
        if ( lastStart >= 0 && lastEnd >= lastStart )
        {
            return pom.substring( lastDelta + lastStart, lastDelta + lastEnd );
        }
        return "";
    }

    /**
     * Sets a mark to the current event.
     *
     * @param index the mark to set.
     */
    public void mark( int index )
    {
        markStart[index] = lastStart;
        markEnd[index] = lastEnd;
        markDelta[index] = lastDelta;
    }

    /**
     * Returns <code>true</code> if nextEnd is including the start of and end element.
     *
     * @return <code>true</code> if nextEnd is including the start of and end element.
     */
    private boolean nextEndIncludesNextEndElement()
    {
        return ( nextEnd > nextStart + 2 && nextEnd - 2 < pom.length() && c( nextEnd - 2 ) == '<' );
    }

    /**
     * Returns <code>true</code> if nextEnd is including the start of the next event.
     *
     * @return <code>true</code> if nextEnd is including the start of the next event.
     */
    private boolean nextEndIncludesNextEvent()
    {
        return nextEnd > nextStart + 1 && nextEnd - 2 < pom.length() &&
            ( c( nextEnd - 1 ) == '<' || c( nextEnd - 1 ) == '&' );
    }

    /**
     * Gets the character at the index provided by the StAX parser.
     *
     * @param index the index.
     * @return char The character.
     */
    private char c( int index )
    {
        return pom.charAt( nextDelta + index );
    }

    /**
     * Replaces the current element with the replacement text.
     *
     * @param replacement The replacement.
     */
    public void replace( String replacement )
    {
        if ( lastStart < 0 || lastEnd < lastStart )
        {
            throw new IllegalStateException();
        }
        int start = lastDelta + lastStart;
        int end = lastDelta + lastEnd;
        if ( replacement.equals( pom.substring( start, end ) ) )
        {
            return;
        }
        pom.replace( start, end, replacement );
        int delta = replacement.length() - lastEnd - lastStart;
        nextDelta += delta;
        for ( int i = 0; i < MAX_MARKS; i++ )
        {
            if ( hasMark( i ) && lastStart == markStart[i] && markEnd[i] == lastEnd )
            {
                markEnd[i] += delta;
            }
        }
        lastEnd += delta;
        modified = true;
    }

    /**
     * Returns <code>true</code> if the specified mark is defined.
     *
     * @param index The mark.
     * @return <code>true</code> if the specified mark is defined.
     */
    public boolean hasMark( int index )
    {
        return markStart[index] != -1;
    }

    public String getBetween( int index1, int index2 )
    {
        if ( !hasMark( index1 ) || !hasMark( index2 ) || markStart[index1] > markStart[index2] )
        {
            throw new IllegalStateException();
        }
        int start = markDelta[index1] + markEnd[index1];
        int end = markDelta[index2] + markStart[index2];
        return pom.substring( start, end );

    }

    /**
     * Replaces all content between marks index1 and index2 with the replacement text.
     *
     * @param index1      The event mark to replace after.
     * @param index2      The event mark to replace before.
     * @param replacement The replacement.
     */
    public void replaceBetween( int index1, int index2, String replacement )
    {
        if ( !hasMark( index1 ) || !hasMark( index2 ) || markStart[index1] > markStart[index2] )
        {
            throw new IllegalStateException();
        }
        int start = markDelta[index1] + markEnd[index1];
        int end = markDelta[index2] + markStart[index2];
        if ( replacement.equals( pom.substring( start, end ) ) )
        {
            return;
        }
        pom.replace( start, end, replacement );
        int delta = replacement.length() - ( end - start );
        nextDelta += delta;

        for ( int i = 0; i < MAX_MARKS; i++ )
        {
            if ( i == index1 || i == index2 || markStart[i] == -1 )
            {
                continue;
            }
            if ( markStart[i] > markStart[index2] )
            {
                markDelta[i] += delta;
            }
            else if ( markStart[i] == markEnd[index1] && markEnd[i] == markStart[index1] )
            {
                markDelta[i] += delta;
            }
            else if ( markStart[i] > markEnd[index1] || markEnd[i] < markStart[index2] )
            {
                markStart[i] = -1;
            }
        }

        modified = true;
    }

    /**
     * Replaces the specified marked element with the replacement text.
     *
     * @param index       The mark.
     * @param replacement The replacement.
     */
    public void replaceMark( int index, String replacement )
    {
        if ( !hasMark( index ) )
        {
            throw new IllegalStateException();
        }
        int start = markDelta[index] + markStart[index];
        int end = markDelta[index] + markEnd[index];
        if ( replacement.equals( pom.substring( start, end ) ) )
        {
            return;
        }
        pom.replace( start, end, replacement );
        int delta = replacement.length() - markEnd[index] - markStart[index];
        nextDelta += delta;
        if ( lastStart == markStart[index] && lastEnd == markEnd[index] )
        {
            lastEnd += delta;
        }
        else if ( lastStart > markStart[index] )
        {
            lastDelta += delta;
        }
        for ( int i = 0; i < MAX_MARKS; i++ )
        {
            if ( i == index || markStart[i] == -1 )
            {
                continue;
            }
            if ( markStart[i] > markStart[index] )
            {
                markDelta[i] += delta;
            }
            else if ( markStart[i] == markStart[index] && markEnd[i] == markEnd[index] )
            {
                markDelta[i] += delta;
            }
        }
        markEnd[index] += delta;
        modified = true;
    }

    public Model parse()
        throws IOException, XmlPullParserException
    {
        MavenXpp3Reader reader = new MavenXpp3Reader();
        return reader.read( new StringReader( pom.toString() ) );
    }

}
